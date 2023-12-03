#include <math.h>

double __exp_finite(double x) { return exp(x); }
double __log_finite(double x) { return log(x); }
double __pow_finite(double x, double y) { return pow(x, y); }

float __expf_finite(float x) { return expf(x); }
float __logf_finite(float x) { return logf(x); }
float __powf_finite(float x, float y) { return powf(x, y); }
