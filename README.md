Run `:findExpects` task and it'll warn you about expects declared in the project.

```shell
./gradlew :findExpects
```

```text
An expect was found: org/example/ExpClass in commonMain
An expect was found: expFun in commonMain
```
