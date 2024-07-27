# wgc-embedded

## Entwicklung

### Run 

First Terminal:
```
./mvnw spring-boot:run -pl *-server -am -Penv-dev 
```

Second Terminal:
```
./mvnw gwt:codeserver -pl *-client -am
```

Or without downloading all the snapshots again:
```
./mvnw gwt:codeserver -pl *-client -am -nsu 
```


## Links

- https://graalvm.github.io/native-build-tools/latest/maven-plugin.html
- https://graalvm.github.io/native-build-tools/latest/maven-plugin-quickstart.html
- https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html