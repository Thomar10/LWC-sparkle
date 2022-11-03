The sparkle in the resource file comes from the zip file, with the following modification to it. A
main have been added as entry point
and the following code have been added as written below. This means
the program accepts a list of integer arguments, and the second last is branch, and last argument is
step. As for now the java program expects max 16 arguments (as the C code implies)

```
int main(int argc, char *argv[]) {
  uint32_t state[argc - 2];

  for (int i = 1; i < argc - 2; ++i) {
    state[i - 1] = atoi(argv[i]);
  }
  int branch = atoi(argv[argc - 2]);
  int steps = atoi(argv[argc - 1]);
  sparkle_opt(state, branch, steps);
  FILE *f = fopen("test", "wt");
  for (int i = 0; i < sizeof(state) / sizeof(state[0]) - 1; ++i) {
    fprintf(f, "%d\n", state[i]);
  }
  fclose(f);
}
```