ADDR_LEN = 20
MEM_SIZE = 2**ADDR_LEN
CACHE_WAY = 4
CACHE_TAG_LEN = 9
CACHE_LINE_SIZE = 128
CACHE_OFFSET_LEN = 7
CACHE_IDX_LEN = ADDR_LEN - CACHE_TAG_LEN - CACHE_OFFSET_LEN
CACHE_SETS_COUNT = 2**CACHE_IDX_LEN
CACHE_SIZE = CACHE_LINE_SIZE * CACHE_WAY * CACHE_SETS_COUNT
CACHE_LINE_COUNT = CACHE_SETS_COUNT * CACHE_WAY

ADDR1_BUS_LEN = ADDR_LEN
ADDR2_BUS_LEN = ADDR_LEN
DATA1_BUS_LEN = 16
DATA2_BUS_LEN = 16
CTR1_BUS_LEN = 3 # /6 ?
CTR2_BUS_LEN = 2 #cache <-> memory /4

CLOCK_LINE_TO = CACHE_LINE_SIZE * 8 / DATA1_BUS_LEN


cache = []
cache_bit = []

cache_lable = []
cache_lable_bit = []

time_used_LRU = []
time_used_bitLRU = []

for i in range(CACHE_SETS_COUNT):
    cache.append([-1] * CACHE_WAY)
    cache_bit.append([-1] * CACHE_WAY)

    cache_lable.append([-1] * CACHE_WAY)
    cache_lable_bit.append([-1] * CACHE_WAY)

    time_used_LRU.append([0] * CACHE_WAY)
    time_used_bitLRU.append([0] * CACHE_WAY)



clock = 0
clock_bit = 0

cache_find_LRU = 0
cache_find_bitLRU = 0

count_all = 0


def get_index(address):
    return (address >> CACHE_OFFSET_LEN) % (1<<CACHE_IDX_LEN)


def get_tag(address):
    return (address >> (CACHE_OFFSET_LEN + CACHE_IDX_LEN))

def change_time_LRU(ind, ind_change_time, time0):
    global time_used_LRU
    for j in range(CACHE_WAY):
        if time_used_LRU[ind][j] > time0:
            time_used_LRU[ind][j] -= 1
    time_used_LRU[ind][ind_change_time] = 3 #самое старшее

def change_time_bitLRU(ind, ind_change_time):
    global time_used_bitLRU
    time_used_bitLRU[ind][ind_change_time] = 1 #самое старшее
    count = 0
    for j in range(CACHE_WAY):
        if time_used_bitLRU[ind][j] == 1:
            count += 1
    if count == 4:
        for j in range(CACHE_WAY):
            if j != ind_change_time:
                time_used_bitLRU[ind][j] = 0


clock_to_memory_w = 2*CLOCK_LINE_TO + 100
    # clock += CLOCK_LINE_TO #время передачи памяти линии с lable = 1
    # clock += 100
    # clock += CLOCK_LINE_TO #передает линию (из запроса) обратно

clock_to_memory = 101 + CLOCK_LINE_TO
    # clock += 1
    # clock += 100
    # clock += CLOCK_LINE_TO #передает линию (из запроса) обратно


def read_LRU(address):
    #address = [tag, ind, offset]
    global cache_find_LRU
    global time_used_LRU
    global clock
    global cache
    ind = get_index(address)
    tag = get_tag(address)
    for i in range(CACHE_WAY):
        if cache[ind][i] == tag: #кэш - попадание
            cache_find_LRU += 1
            time_0_LRU = time_used_LRU[ind][i]
            change_time_LRU(ind, i, time_0_LRU)


            clock += 6
            return 1

    #не попоадание
    min_time = 4
    min_ind = -1
    clock += 4
    for i in range(CACHE_WAY):
        if time_used_LRU[ind][i] < min_time:
            min_time = time_used_LRU[ind][i]
            min_ind = i

    if cache_lable[ind][min_ind] < 1: #если есть возмодность не записывать в оперативку
        clock += clock_to_memory #пересчет тактов линия <- память
        cache[ind][min_ind] = tag
        change_time_LRU(ind, min_ind, min_time)
        cache_lable[ind][min_ind] = 0

        return 0 #промах

    #1 - lable
    clock += clock_to_memory_w #пересчет тактов линия (записать) <- память (tag)
    cache[ind][min_ind] = tag
    change_time_LRU(ind, min_ind, min_time)
    cache_lable[ind][min_ind] = 0

    return 0 #промах

def read_bitLRU(address):
    #address = [tag, ind, offset] - 3 int числа?
    global cache_find_bitLRU
    global time_used_bitLRU
    global clock_bit
    global cache_bit
    ind = get_index(address)
    tag = get_tag(address)
    for i in range(CACHE_WAY):
        #print(ind, i, address, len(bin(address)))
        if cache_bit[ind][i] == tag: #кэш - попадание
            cache_find_bitLRU += 1 ##можно удалить+---------------------
            change_time_bitLRU(ind, i)

            clock_bit += 6
            return 1

    #не попоадание
    #write_to_cache()
    min_ind = 0
    clock_bit += 4
    for i in range(CACHE_WAY):
        if time_used_bitLRU[ind][i] != 1:
            min_ind = i

    if cache_lable_bit[ind][min_ind] < 1: #если есть возможность не записывать в оперативку старое | 0/1
        clock_bit += clock_to_memory #пересчет тактов линия <- память
        cache_bit[ind][min_ind] = tag
        change_time_bitLRU(ind, min_ind)
        cache_lable_bit[ind][min_ind] = 0

        return 0 #промах

    clock_bit += clock_to_memory_w #пересчет тактов линия прошлая (записать) <-> память новая линия (tag)
    cache_bit[ind][min_ind] = tag
    change_time_bitLRU(ind, min_ind)
    cache_lable_bit[ind][min_ind] = 0 #взяли из оперативки по Tag

    return 0 #промах

# cache <-> CPU clock - вне фунции обработать

def write_LRU(address):
    global cache_find_LRU
    global time_used_LRU
    global clock
    global cache
    ind = get_index(address)
    tag = get_tag(address)
    for i in range(CACHE_WAY):
        if cache[ind][i] == tag: #кэш - попадание
            cache_lable[ind][i] = 1
            cache_find_LRU += 1
            time_0_LRU = time_used_LRU[ind][i]
            change_time_LRU(ind, i, time_0_LRU)
            clock += 6

            return 1

    #не попоадание
    #write_to_cache()
    min_time = 4
    min_ind = -1
    clock += 4
    #print(">>>W ", clock)
    for i in range(CACHE_WAY):
        if time_used_LRU[ind][i] < min_time:
            min_time = time_used_LRU[ind][i]
            min_ind = i
    if cache_lable[ind][min_ind] < 1: #если не перезаписывать в оперативку
        cache[ind][min_ind] = tag
        change_time_LRU(ind, min_ind, min_time)
        cache_lable[ind][min_ind] = 1
        return 0

    #lable = 1
    clock += clock_to_memory #пересчет тактов линия (записать) -> память
    cache[ind][min_ind] = tag
    cache_lable[ind][min_ind] = 1
    change_time_LRU(ind, min_ind, min_time)

    return 0 #промах

def write_bitLRU(address):
    global cache_find_bitLRU
    global time_used_bitLRU
    global clock_bit
    global cache_bit
    ind = get_index(address)
    tag = get_tag(address)
    for i in range(CACHE_WAY):
        if cache_bit[ind][i] == tag: #кэш - попадание
            cache_lable_bit[ind][i] = 1
            cache_find_bitLRU += 1
            change_time_bitLRU(ind, i)
            clock_bit += 6

            return 1

    #не попоадание
    min_ind = -1
    clock_bit += 4
    for i in range(CACHE_WAY):
        if time_used_bitLRU[ind][i] != 1:
            min_ind = i
            break

    if cache_lable_bit[ind][min_ind] < 1: #если не перезаписывать в оперативку
        cache_bit[ind][min_ind] = tag
        cache_lable_bit[ind][min_ind] = 1
        change_time_bitLRU(ind, min_ind)
        return 0

    clock_bit += clock_to_memory #пересчет тактов линия (записать) -> память
    cache_bit[ind][min_ind] = tag
    cache_lable_bit[ind][min_ind] = 1
    change_time_bitLRU(ind, min_ind)

    return 0 #промах


clock_common = 0

M = 64
N = 60
K = 32
clock_common += 3

PA = 0x40000
PB = M*K + PA
PC = PB + K*N*2

clock_common+=1 #pa
clock_common+=1 # pc

clock_common += 1 #y
for y in range(M):
    clock_common += 1 # y++

    clock_common += 1 #x
    for x in range(N):
        clock_common += 1 # x++

        clock_common += 1 #pb
        clock_common += 1 #s

        clock_common += 1 #k
        for k in range(K):
            clock_common += 1 # k++

            clock_common += 1 #делаем запрос в кэш(адрес передаем)
            read_bitLRU(y*K + k + PA) # pa[k]
            read_LRU(y*K + k + PA) # pa[k]
            count_all += 1
            clock_common += 1 #8 // DATA1_BUS_LEN  типа 8 округлям к 1 - принимаем данные


            clock_common += 1 #делаем запрос в кэш(адрес передаем)
            read_LRU(k*N*2 + x*2 + PB) # pb[k]
            read_bitLRU(k*N*2 + x*2 + PB) # pb[k]
            count_all += 1
            clock_common += 1 #16 // DATA1_BUS_LEN

            clock_common += 5 # *
            clock_common += 1 # +

        clock_common += 2 # 32/DATA1_BUS_LEN
        write_LRU((y*N + x)*4 + PC) # pc
        write_bitLRU(4*(y*N + x) + PC) # pc
        count_all += 1
        clock_common += 1 #обратно передавать CPU

    clock_common += 1 # pa+=k
    clock_common += 1 #pc+=n

clock_common += 1 #выход из ф-ии

clock_bit += clock_common
clock += clock_common


print(f"LRU:\thit perc. {(cache_find_LRU / count_all) * 100:3.4f}%\ttime: {(clock)}\npLRU:\thit perc. {(cache_find_bitLRU / count_all) * 100:3.4f}%\ttime: {clock_bit}")
