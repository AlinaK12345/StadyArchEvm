import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    static List<Integer> elf = new ArrayList<>();
    static StringBuilder symtable = new StringBuilder("\nSymbol Value              Size Type     Bind     Vis       Index Name\n");
    static StringBuilder text = new StringBuilder();
    static HashMap<Integer, String> label= new HashMap<>();
    static int lastLabel = 0;
    private static int SECTION_HEADER_POSITION = 32; // 32-35
    private static int NUMBER_OF_SECTIONS; // 48-49
    private static int LEN_SECTION = 40; // 1
    private static int sectionHeadPosition = 0;
    private static int SECTION_HEADER_NAMES; // 50-51
    private static int TEXT_ADDRESS;
    private static int LEN_TEXT_SECTION;
    private static int TEXT_START_VIRTUAL_ADDRESS;
    private static int LEN_SYMTABLE_SECTION;
    private static int SYMTABLE_ADDRESS;
    private static int SYMBOL_TABLE_STR_ADDRESS;
    public static void main(String[] args) {
        String filenameIn = args[0];
        String filenameOut = args[1];
        try{
            InputStream in =
                    new FileInputStream(filenameIn);
            try{
                solution(in);

            } finally {
                in.close();

            }
            OutputStreamWriter out = new OutputStreamWriter(
                    new FileOutputStream(filenameOut), "UTF-8");
            try {
                out.write(".text\n");
                out.write(text.toString());
                out.write("\n\n");
                out.write(".symtab\n");
                out.write(symtable.toString());
            } finally {
                out.close();
            }

        } catch (FileNotFoundException e){
            System.out.println("No Input/Output file");
        } catch (IOException e) {
            System.out.println("IOExeption");
        }
    }

    private static void checkIsCorrect(){
        if (elf.get(0) != 0x7f || elf.get(1) != 0x45 || elf.get(2) != 0x4c || elf.get(3) != 0x46) {
            throw new UnsupportedOperationException("Unsupported file format");
        }
        if (elf.get(4) != 1) {
            throw new UnsupportedOperationException("Not 32 bits file");
        }
        if (elf.get(5) != 1) {
            throw new UnsupportedOperationException("Not little-endian file");
        }
    }

    private static int makeBitNumber(int pos, int len){
        int ans = 0;
        for (int i = len-1; i >=0; i--){
            ans = ans << 8;
            ans += elf.get(pos+i);
        }
        return ans;
    }

    private static void checkAddressStr(int currentPos){
        int address = makeBitNumber(currentPos, 4) + SECTION_HEADER_NAMES; //
        int a = elf.get(address);
        StringBuilder str = new StringBuilder();
        int i = 0;
        while (a != 0){
            str.append((char) a);
            i++;
            a = elf.get(address + i);
        }
        if (str.toString().equals(".text")){
            TEXT_START_VIRTUAL_ADDRESS = makeBitNumber(currentPos + 0x0C, 4);
            TEXT_ADDRESS = makeBitNumber(currentPos + 0x10, 4); //
            LEN_TEXT_SECTION = makeBitNumber(currentPos + 0x14, 4); //

        }
        else if (str.toString().equals(".symtab")){
            SYMTABLE_ADDRESS = makeBitNumber(currentPos + 0x10, 4);
            LEN_SYMTABLE_SECTION = makeBitNumber(currentPos + 0x14, 4);
        }
        else if (str.toString().equals(".strtab")) {
            SYMBOL_TABLE_STR_ADDRESS = makeBitNumber(currentPos + 0x10, 4);
        }
    }

    private static int getBitNumber(int number, int begin, int end){
        int ans = 0;
        int a = 0;
        for (int i = end; i <= begin; i++) {
            if (((number >> i) & 1) == 1) {
                ans |= (1 << i);
            }
        }

        return ans >> end;
    }

    private static int putBits(int number, int begin, int end, int bits){
        int ans = number % (1 << end); //
        ans += (number >> begin) << begin;
        ans += bits << end;
        return ans;
    }



    private static void readText(int i, int parametr){

        int address = i + TEXT_ADDRESS;
        int virtAddress = TEXT_START_VIRTUAL_ADDRESS + i;
        StringBuilder command = new StringBuilder();
        int number = makeBitNumber(address, 4);
        int opcode = getBitNumber(number, 6, 0);


        String name;

        int funct3 = getBitNumber(number, 14, 12);
        int funct7 = getBitNumber(number, 31, 25);

        int rs1 = getBitNumber(number, 19, 15);
        int rs2 = getBitNumber(number, 24, 20);
        int rd = getBitNumber(number, 11, 7);

        List<Integer> registrs = new ArrayList<>();
        registrs.add(rd);
        registrs.add(rs1);

        int immediate = -1;

        switch (opcode){
            case 0b0110111 -> {
                name = "lui";
                registrs.remove(1);
                immediate = getBitNumber(number, 31, 12);
            }
            case 0b0010111 -> {
                name = "auipc";
                registrs.remove(1);

                immediate = getBitNumber(number, 31, 12);
            }
            case 0b1101111 -> {
                name = "jal";
                registrs.remove(1);

                immediate = 0;
                immediate = putBits(immediate, 19, 12, getBitNumber(number, 19, 12));
                immediate = putBits(immediate, 11, 11, getBitNumber(number, 20, 20));
                immediate = putBits(immediate, 10, 1, getBitNumber(number, 30, 21));
                immediate = putBits(immediate, 20, 20, getBitNumber(number, 31, 31));
//
                immediate += virtAddress;
                makeLabel(immediate);

            }
            case 0b1100111 -> {
                name = "jalr";
                immediate = getBitNumber(number, 31, 20);

            }
            case 0b1100011 ->{
                switch (funct3){
                    case 0b000 -> name = "beq";
                    case 0b001 -> name = "bne";
                    case 0b100 -> name = "blt";
                    case 0b101 -> name = "bge";
                    case 0b110 -> name = "bltu";
                    case 0b111 -> name = "bgeu";
                    default -> name = "invalid_instruction";
                }
                registrs.remove(0);
                registrs.add(rs2);

                immediate = 0;
                immediate = putBits(immediate, 12, 12, getBitNumber(number, 31, 31));
                immediate = putBits(immediate, 11, 11, getBitNumber(number, 7, 7));
                immediate = putBits(immediate, 10, 5, getBitNumber(number, 30, 25));
                immediate = putBits(immediate, 4, 1, getBitNumber(number, 11, 8));


                immediate += virtAddress;
                makeLabel(immediate);
            }
            case 0b0000011 ->{
                switch (funct3){
                    case 0b000 -> name = "lb";
                    case 0b001 -> name = "lh";
                    case 0b010 -> name = "lw";
                    case 0b100 -> name = "lbu";
                    case 0b101 -> name = "lhu";
                    default -> name = "invalid_instruction";
                }
//
                immediate = getBitNumber(number, 31, 20);
            }
            case 0b0100011 -> {
                switch (funct3){
                    case 0b000 -> name = "sb";
                    case 0b001 -> name = "sh";
                    case 0b010 -> name = "sw";
                    default -> name = "invalid_instruction";
                }
                registrs.set(0, rs2);
                immediate = getBitNumber(number, 31, 25);
                immediate = immediate << 5;
                immediate += getBitNumber(number, 11, 7);
            }
            case 0b0010011 -> {
                switch (funct3) {
                    case 0b000 -> name = "addi";
                    case 0b010 -> name = "slti";
                    case 0b011 -> name = "sltiu";
                    case 0b100 -> name = "xori";
                    case 0b110 -> name = "ori";
                    case 0b111 -> name = "andi";
                    case 0b001 -> {
                        name = "slli";
                        int shamt = getBitNumber(number, 24,20);

                    }
                    case 0b101 -> {
                        switch (funct7) {
                            case 0b0000000 -> name = "srli";
                            case 0b0100000 -> name = "srai";
                            default -> name = "invalid_instruction";
                        }
                        int shamt = getBitNumber(number, 24,20);
                    }
                    default -> name = "invalid_instruction";
                }
                immediate = getBitNumber(number, 31, 20);
                if (funct3 ==0b101 ){
                    immediate = getBitNumber(number, 24,20); // shamt
                }
            }
            case 0b0110011 -> {

                if (funct7 == 0b0000001){   /// RV32M
                    switch (funct3) {
                        case 0b000 -> name = "mul";
                        case 0b001 -> name = "mulh";
                        case 0b010 -> name = "mulhsu";
                        case 0b011 -> name = "mulhu";
                        case 0b100 -> name = "div";
                        case 0b101 -> name = "divu";
                        case 0b110 -> name = "rem";
                        case 0b111 -> name = "remu";
                        default -> name = "invalid_instruction";
                    }
                }
                else {
                    switch (funct3) {
                        case 0b000 -> {
                            switch (funct7) {
                                case 0b0000000 -> name = "add";
                                case 0b0100000 -> name = "sub";
                                default -> name = "invalid_instruction";
                            }
                        }
                        case 0b001 -> name = "sll";
                        case 0b010 -> name = "slt";
                        case 0b011 -> name = "sltu";
                        case 0b100 -> name = "xor";
                        case 0b101 -> {
                            switch (funct7) {
                                case 0b0000000 -> name = "srl";
                                case 0b0100000 -> name = "sra";
                                default -> name = "invalid_instruction";
                            }
                        }
                        case 0b110 -> name = "or";
                        case 0b111 -> name = "and";
                        default -> name = "invalid_instruction";
                    }
                }
                immediate = -1;
                registrs.add(rs2);
            }
            case 0b0001111 -> {
                switch (getBitNumber(number, 31, 28)) {
                    case 0b1000 -> {
                        name = "fence.tso";
                        registrs.remove(1);
                        registrs.remove(0);
                    }
                    case 0b0000 -> {
                        name = "fence";
                        registrs.remove(1);
                        registrs.remove(0);
                    }
                    default -> name = "invalid_instruction";
                }
                immediate = -1;
            }
            case 0b1110011 -> {
                switch (getBitNumber(number, 31, 20)){
                    case 0b000000000000 -> {
                        name = "ecall";
                        registrs.remove(1);
                        registrs.remove(0);
                    }
                    case 0b000000000001 -> {
                        name = "ebreak";
                        registrs.remove(1);
                        registrs.remove(0);
                    }
                    default -> name = "invalid_instruction";
                }
                immediate = -1;
            }
            default ->  name = "invalid_instruction";
        }

        if (parametr == 1) {
            makeStringText(virtAddress, number, name, registrs, immediate);
        }
    }

    private static void makeLabel(int virtAddr){
        if (!label.containsKey(virtAddr)){
            label.put(virtAddr, "L" + lastLabel);
            lastLabel++;
        }

    }

    private static void makeStringText(int virtAddr, int number, String name, List<Integer> registrs, int immediate){
        if (name.equals("invalid_instruction")){
            text.append(String.format("   %05x:\t%08x\t%-7s\n", virtAddr, number, name));
        }
        else{
            if (label.containsKey(virtAddr)) {
                text.append(String.format("\n%08x \t<%s>:\n", virtAddr, label.get(virtAddr)));
                if (label.get(virtAddr).charAt(0) != 'L'){
                }
            }
             if (name.equals("fence")){
                text.append(String.format("   %05x:\t%08x\t%7s\t%s, %s\n",
                        virtAddr, number, name, "iorw", "iorw"));
            }
            else if (name.equals("jal")){
                text.append(String.format("   %05x:\t%08x\t%7s\t%s, 0x%x <%s>\n", virtAddr,number,
                        name, registerName(registrs.get(0)), immediate, label.get(immediate)));
            }
            else if (name.equals("jalr") || name.length() == 2 &&(name.charAt(0) == 's' || name.charAt(0) == 'l') || name.equals("lbu") || name.equals("lhu")){
                text.append(String.format("   %05x:\t%08x\t%7s\t%s, %d(%s)\n",
                        virtAddr, number, name, registerName(registrs.get(0)), immediate, registerName(registrs.get(1))));
            }
            else if ( name.charAt(0) == 'b'){
                text.append(String.format("   %05x:\t%08x\t%7s\t%s, %s, 0x%x, <%s>\n", virtAddr,number,
                        name, registerName(registrs.get(0)),registerName(registrs.get(1)), immediate, label.get(immediate)));
            }
            else if (registrs.size() == 3){
                text.append(String.format("   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        virtAddr, number, name, registerName(registrs.get(0)), registerName(registrs.get(1)), registerName(registrs.get(2))));
            }
            else if (registrs.size() == 2){
                text.append(String.format("   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        virtAddr, number, name, registerName(registrs.get(0)),registerName(registrs.get(1)), immediate));
            }
            else if (registrs.size() == 1){
                if (name.equals("lui") || name.equals("auipc")){
                    text.append(String.format("   %05x:\t%08x\t%7s\t%s, %s\n",
                            virtAddr, number, name, registerName(registrs.get(0)), "0x" + Integer.toHexString(immediate)));
                }
                else {
                    text.append(String.format("   %05x:\t%08x\t%7s\t%s, %s\n",
                            virtAddr, number, name, registerName(registrs.get(0)), immediate));
                }
            }
            else if (registrs.size() == 0){
                text.append(String.format("   %05x:\t%08x\t%7s\n",
                        virtAddr, number, name));
            }
        }
    }



    private static String registerName(int reg) {
        return switch (reg) {
            case 0 -> "zero";
            case 1 -> "ra";
            case 2 -> "sp";
            case 3 -> "gp";
            case 4 -> "tp";
            case 5 -> "t0";
            case 6 -> "t1";
            case 7 -> "t2";
            case 8 -> "s0";
            case 9 -> "s1";
            case 10 -> "a0";
            case 11 -> "a1";
            case 12 -> "a2";
            case 13 -> "a3";
            case 14 -> "a4";
            case 15 -> "a5";
            case 16 -> "a6";
            case 17 -> "a7";
            case 18 -> "s2";
            case 19 -> "s3";
            case 20 -> "s4";
            case 21 -> "s5";
            case 22 -> "s6";
            case 23 -> "s7";
            case 24 -> "s8";
            case 25 -> "s9";
            case 26 -> "s10";
            case 27 -> "s11";
            case 28 -> "t3";
            case 29 -> "t4";
            case 30 -> "t5";
            case 31 -> "t6";
            default -> throw new UnsupportedOperationException("not correct register");
        };
    }

    private static void readSymtable(int i){
        int address = SYMTABLE_ADDRESS + i;

        int st_name = makeBitNumber(address, 4);
        int st_value = makeBitNumber(address + 4, 4);
        int st_size = makeBitNumber(address + 8, 4);
        int st_info = makeBitNumber(address + 12, 1);
        int st_other = makeBitNumber(address + 13, 1);
        int st_shndx = makeBitNumber(address + 14, 2);

        address = SYMBOL_TABLE_STR_ADDRESS + st_name;
        int a = elf.get(address);
        StringBuilder str = new StringBuilder();
        int j = 0;
        while (a != 0){ //
            str.append((char) a);
            j++;
            a = elf.get(address + j);
        }
        String name = str.toString();

        int value = st_value;
        int bind = ((st_info) >> 4);
        int type = ((st_info) & 0xf);
        int vis = st_other & 0x3;

        if (!name.isEmpty()) {
            label.put(value, name);
        }
        makeStringSymtable(i, value, st_size, type, bind, vis, st_shndx, name);

    }

    private static void makeStringSymtable(int i, int value, int size, int type, int bind, int vis, int index, String name){
        String line = String.format("[%4d] 0x%-15X %5d %-8s %-8s %-8s %6s %s\n",i/16, value,
                size, getType(type), getBind(bind),
                getVisiable(vis), getIndex(index), name);
        symtable.append(line);
    }



    private static String getIndex(int ind){
        return switch (ind) {
            case 0 -> "UNDEF";
            case 0xff00 -> "LORESERVE"; //
            case 0xff01 -> "AFTER";
            case 0xff02 -> "AMD64_LCOMMON";
            case 0xff1f -> "HIPROC";
            case 0xff20 -> "LOOS";
            case 0xff3f -> "LOSUNW"; //
            case 0xfff1 -> "ABS";
            case 0xfff2 -> "COMMON";
            case 0xffff -> "XINDEX"; //
            default -> Integer.toString(ind);
        };
    }

    private static String getVisiable(int vis){
        return switch(vis) {
            case 0 -> "DEFAULT";
            case 1 -> "INTERNAL";
            case 2 -> "HIDDEN";
            case 3 -> "PROTECTED";
            case 4 -> "EXPORTED";
            case 5 -> "SINGLETON";
            case 6 -> "ELIMINATE";
            default -> {
                throw new UnsupportedOperationException("Unsupported symtab segment visibility");
            }
        };
    }

    private static String getType(int type){
        return switch(type) {
            case 0 -> "NOTYPE";
            case 1 -> "OBJECT";
            case 2 -> "FUNC";
            case 3 -> "SECTION";
            case 4 -> "FILE";
            case 5 -> "COMMON";
            case 6 -> "TLS";
            case 10 -> "LOOS";
            case 12 -> "HIOS";
            case 13 -> "LOPROC";
            case 15 -> "HIPROC";
            default -> {
                throw new UnsupportedOperationException();
            }
        };
    }
    private static String getBind(int bind){
        return switch(bind) {
            case 0 -> "LOCAL";
            case 1 -> "GLOBAL";
            case 2 -> "WEAK";
            case 10 -> "LOOS";
            case 12 -> "HIOS";
            case 13 -> "LOPROC";
            case 15 -> "HIPROC";
            default -> {
                throw new UnsupportedOperationException("Unsupported symtab segment bind");
            }
        };
    }


    private static void solution(InputStream in) throws IOException {
        int read = in.read();
        while (read != -1) {
            elf.add(read);
            read = in.read();
        }
        checkIsCorrect();

        sectionHeadPosition = makeBitNumber(SECTION_HEADER_POSITION, 4);
        NUMBER_OF_SECTIONS = makeBitNumber(48, 2);
        SECTION_HEADER_NAMES = makeBitNumber(sectionHeadPosition + LEN_SECTION * makeBitNumber(50, 2) + 0x10, 4);


        int currentPos = sectionHeadPosition;
        for (int i = 0; i < NUMBER_OF_SECTIONS; i++) {
            checkAddressStr(currentPos);
            currentPos += LEN_SECTION;

        }


        for (int i = 0; i < LEN_SYMTABLE_SECTION; i += 16) {
            readSymtable(i);
        }

        for (int i = 0; i < LEN_TEXT_SECTION; i += 4) {
            readText(i, 0);
        }
        for (int i = 0; i < LEN_TEXT_SECTION; i += 4) {
            readText(i, 1);
        }

    }



}
