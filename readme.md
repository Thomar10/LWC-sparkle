The following project contains all the code for the LWC-SPARKLE. This includes the SPARKLE
permutation, the SCWAEMM-cipher, and ESCH-hash.
Furthermore, we have implemented masked versions of both SPARKLE, SCHWAEMM, and ESCH using a bunch
of different strategies, namely conversion and computing masked addition on boolean shares.

All implementations are checked to have the same behavior as the C code by simply looking at both
codes' outputs.

Benchmark of the codes can be found under test/benchmarks/handin using JMH-benchmarks