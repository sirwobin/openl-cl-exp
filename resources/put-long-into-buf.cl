__kernel void put_long_into_buf(long seed, __global long* buf) {
    buf[0] = seed;
}