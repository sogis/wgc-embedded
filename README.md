# wgc-embedded

## Entwicklung

- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte&c=2600779,1215505&z=15
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte&c=2600779,1215505&s=22694
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D&c=2600779,1215505&s=22694

- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.agi.av.grundstuecke,ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte&c=2600779,1215505&s=22694 

- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte,wms%3Ahttps%3A%2F%2Fwfs.geodienste.ch%2Fav_0%2Fdeu%23Hoheitsgrenzen%5B35%5D&c=2600779,1215505&s=22694

Achtung: Es darf keine "&" beim externen WMS vorkommen. Auch wenn es encodiert ist.

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