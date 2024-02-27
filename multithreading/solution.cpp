#define _CRT_SECURE_NO_WARNINGS
#include <stdio.h>
#include <omp.h>
#include <cmath>
#include <algorithm>
#include <iostream>
#include <string>
using namespace std;

float distance(float* a, float* b) {
    return std::sqrt(std::pow(a[0] - b[0], 2) + std::pow(a[1] - b[1], 2) + std::pow(a[2] - b[2], 2));
}

const float eps = 0.000001;

bool equal_d(float a, float b) {
    return std::abs(a - b) < eps;
}


long long r_max = 4294967296;
float random_f(float a, long seed) { 
    
    float f = (seed % r_max) * (2 * (a)) / (r_max - 1) - a;
    
    return f;
}
long a1 = 2147483647;
const long m1 = 16807;
long new_seed(long seed) {
   
    seed = 214013 * seed + 2531011;
    seed ^= seed >> 15;
    
    return seed ;
    
}

int main(int argc, char** argv)
{
    if (argc != 4) {
        std::cerr << "Not correct input" << std::endl;
        exit(1);
    }
   
    FILE* fin = fopen(argv[2], "rb");
    if (fin == NULL) {
        std::cerr << "No file" << std::endl;
        exit(1);
    }
    long long int n;
    //cout << "ok";

    int threads = std::stoi(argv[1]);//to number

    float coords[3][3];
    float x, y, z;
    fscanf(fin, "%lld\n", &n);
    //cout << n << '\n';

    for (int i = 0; i < 3; i++) {
        fscanf(fin, "(%f %f %f)\n", &x, &y, &z);
        coords[i][0] = x;
        coords[i][1] = y;
        coords[i][2] = z;
    }
    //cout << coords[0][0]<<" " << coords[0][1];
    fclose(fin);
    //cout << "ok" << '\n';
    //analitic:
    float l1 = distance(coords[0], coords[1]);
    float l2 = distance(coords[2], coords[1]);
    //float l3 = distance(coords[0], coords[2]);
    float a = std::min(l1, l2);
    float ans1 = a*a*a * std::sqrt(2) / 3;
    //cout << l1 << '\n';
    //

    float a_2 = std::sqrt(2) * a / 2;

    float tstart = omp_get_wtime();

    //cout << tstart << '\n';
    int sum = 0;
    int count_in = 0;
    int inside = 1;
    int output_threads = 0;
    
    if (threads == -1) {
        unsigned long seed = 1;
        float x1, y1, z1;
        for (int i = 0; i < n; ++i)
        {
            x1 = random_f(a_2, seed);
            seed = new_seed(seed);
            //cout << i << " " << seed << ' ' << x1 << '\n';
            //cout << omp_get_thread_num() << '\n';
            y1 = random_f(a_2, seed);
            seed = new_seed(seed);
            z1 = random_f(a_2, seed);
            seed = new_seed(seed);
            if (abs(x1) + abs(y1) + abs(z1) <= a_2) {
                count_in += 1;
            }

        }
    }
    else {
#ifdef _OPENMP
        if (threads > 0) {
            omp_set_num_threads(threads); // count of threads
        }
#endif

#pragma omp parallel
        {
#ifdef _OPENMP
            unsigned long seed = omp_get_thread_num() + 1;
            //cout << omp_get_thread_num()<<'\n';

            int local_in = 0;
            float x1, y1, z1;
#pragma omp for nowait schedule(static)
            for (int i = 0; i < n; ++i)
            {
                //cout << seed << '\n';
                x1 = random_f(a_2, seed);
                seed = new_seed(seed);
                //cout << i << " " << seed << ' ' << x1 << '\n';
                //cout << omp_get_thread_num() << '\n';
                y1 = random_f(a_2, seed);
                seed = new_seed(seed);
                z1 = random_f(a_2, seed);
                seed = new_seed(seed);
                if (abs(x1) + abs(y1) + abs(z1) <= a_2) {
                    local_in += 1;
                }

            }
#pragma omp atomic
            count_in += local_in;
            output_threads += 1;
            
#endif
        }
    }
    
    float ans2 = a * (2 * sqrt(2) * a * a* count_in) / n;

    float tend = omp_get_wtime();

    FILE* fout = fopen(argv[3], "w");
    fprintf(fout, "%g %g\n", ans1, ans2);
    fclose(fout);
    //cout << ans1<<" "<<ans2 << '\n';
    //printf("Time (sec): %f\n", tend - tstart);
    
    printf("Time(% i thread(s)) : %g ms\n", output_threads, (tend - tstart)*100);

    return 0;
}
