# wgc-embedded

WGC embedded ist ein Web "Map" Client, der als iFrame in Webseiten integriert werden kann. Er ist somit für den Benutzer über die URL steuerbar und konfigurierbar. Es stehen alle Layer und Hintergrundkarten des "grossen Bruders" (unsere Web GIS Clients) zur Verfügung. Ebenso können externe WMS-Layer hinzugefügt werden. Objektabfragen werden unterstützt.

## Benutzung

### Funktionen
Es können die Hintergrundkarten unseres WMTS ausgewählt (`bl=`) werden. Es stehen sämtliche Kartenebenen unseres WMS zur Verfügung. Ebenfalls können externe WMS-Layer hinzugeladen werden (siehe Beispiel-Requests). Objektinformationen von externe Kartenebenen werden nicht unterstützt. Ein Link verweist auf den "Original"-Web-GIS-Client. 

### Parameter

In der zweiten Version wurden die Parameter an die Parameter des Web 

- Die Basis-URL lautet: https://geo.so.ch/api/embed/v2/index.html
- bl: Optional. Wählt die Hintergrundkarte aus. Es stehen die gleichen Hintergrundkarten wie im Web GIS Client zur Verfügung. Die Namen entsprechen den WMTS-Layernamen.
- l: Optional. Kommaseparierte Liste für die anzuzeigenden Kartenebenen. Die Namen entsprechen den WMS-Layername. Der WMS-Layername entspricht dem Identifier wie er auch in der URL des Web GIS Clients für die jeweilige Kartenebene sichtbar ist. In encodierten, eckigen Klammern kann die Transparenz gesteuert werden. Mit einam "!" am Ende des Namens ist der Layer nicht sichtbar (wird aber so an den grossen Web GIS Client mitgeliefert). Externe WMS können mit `wms:<Endpunkt>#<Layername>` eingebunden werden (siehe Beispiele).
- c: Optional. Koordinatewerte des Kartenmittelpunktes (kommasepariert).
- s: Optional. Massstab der Karte. 

### Beispiele

- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte&c=2600779,1215505&z=15
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte&c=2600779,1215505&s=22694
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D&c=2600779,1215505&s=22694
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.agi.av.grundstuecke,ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte&c=2600779,1215505&s=22694 
- http://localhost:8080/?bl=ch.so.agi.hintergrundkarte_ortho&l=ch.so.afu.ewsabfrage.abfrage%5B50%5D,ch.so.afu.gewaesserschutz.schutzareale%21,ch.so.afu.gewaesserschutz.schutzzonen,ch.so.afu.altlasten.standorte,wms%3Ahttps%3A%2F%2Fwfs.geodienste.ch%2Fav_0%2Fdeu%23Hoheitsgrenzen%5B35%5D&c=2600779,1215505&s=22694

Achtung: Es darf kein "&" beim externen WMS vorkommen. Auch wenn es encodiert ist.

## Konfigurieren und Starten

### Optionen (Umgebungsvariablen)

| Name | Beschreibung | Standard |
|-----|-----|-----|
| `CONFIG_RESOURCE` | Resourcen-Pfad zur config.json-Datei. Falls eine Datei ausserhalb der Anwendung verwendet werden soll, muss `file:` verwendet werden. In der Anwendung sind "-prod", "-int" und "-test" vorhanden. | `classpath:/config.json` |

### JVM

```
java -jar wgc-embedded-server/target/wgc-embedded-exec.jar
```

### Native

```
CONFIG_RESOURCE=file:/Users/stefan/tmp/config.json ./wgc-embedded-server/target/wgc-embedded-server
```
Falls man von aussen das config.json setzen will. Ohne diese Angabe wird eine interne config.json-Datei verwendet.

### Docker (nur Native Image)

```
docker run -p8080:8080 -e CONFIG_RESOURCE=classpath:/config-prod.json sogis/wgc-embedded 
```

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

Falls der Client (der Codeserver) nicht startet und es eine Fehlermeldung bezüglich eine bereits verwendeten Ports wirf, kann man den Codeserver auch abschiessen:

```
netstat -vanp tcp | grep 9876
```

Und anschliessendes `kill -9 <PID>`.

### Build

#### JVM

```
./mvnw -Penv-prod package -DskipTests
```

#### Native

```
./mvnw -Penv-prod,native package -DskipTests
```

### Test

```
./mvnw test
```

Siehe dazu v.a. auch das Github Action Yaml. 

## Konfiguration und Betrieb in der GDI
- Openshift-Template:
- nginx-conf: 
- Liveness- und Readyness-Probe:
  * http://localhost:8080/actuator/health/livenessState 
  * http://localhost:8080/actuator/health/readinessState

## Interne Struktur

Spring Boot (also Tomcat) wird vor allem für das Ausliefern des Javascriptes und der Webseite verwendet. Sonstige Businesslogik ist serverseitg kaum vorhanden. Einzig die Konfiguration (siehe ConfigController) der Anwendung wird mit Spring Boot gemacht.

## Links

- https://graalvm.github.io/native-build-tools/latest/maven-plugin.html
- https://graalvm.github.io/native-build-tools/latest/maven-plugin-quickstart.html
- https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html
- https://app.gitter.im/#/room/#gwtproject_gwt:gitter.im