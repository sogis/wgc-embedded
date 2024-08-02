# wgc-embedded

1. Map Element
2. onChangeBasemap
3. Wmts-/WmsManager 
- wo ist layer add? https://gitlab.com/geogirafe/gg-viewer/-/blob/main/src/components/map/tools/wmtsmanager.ts?ref_type=heads
onChangeBasemap in in Map-Komponente. basemap ist ein Model.


## Entwicklung

- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte

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

Falls der Client (der Codeserver) nicht startet und es eine Fehlermeldung bezüglich eine bereits verwendeten Ports wirf, kann man den Codeserver auch abschiessen:

```
netstat -vanp tcp | grep 9876
```

Und anschliessendes `kill -9 <PID>`.


## Links

- https://graalvm.github.io/native-build-tools/latest/maven-plugin.html
- https://graalvm.github.io/native-build-tools/latest/maven-plugin-quickstart.html
- https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html
- https://app.gitter.im/#/room/#gwtproject_gwt:gitter.im