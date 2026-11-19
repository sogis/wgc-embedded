# QWC2 Embedded

Anonymer SO!GIS-Kartenviewer für iframes. Der Einstieg bleibt `/api/embed/v2/index.html`.

## Docker-Image 3.1

| Registry | Image | Tags |
| --- | --- | --- |
| Docker Hub | `sogis/wgc-embedded` | `3`, `3.1`, `3.1.<github.run_id>`, `latest` |
| GitHub Container Registry | `ghcr.io/sogis/wgc-embedded` | `3`, `3.1`, `3.1.<github.run_id>`, `latest` |

Beide Registries erhalten ein Multiarch-Image für `linux/amd64` und `linux/arm64`. `3`, `3.1` und `latest` folgen der neuesten erfolgreichen Veröffentlichung. Der Tag mit der GitHub-Actions-Run-ID bezeichnet den Workflow-Lauf. Ein erneuter Versuch desselben Laufs verwendet dieselbe ID.

Der Container lauscht standardmässig auf Port `8080`. `GET /actuator/health`, `GET /actuator/health/livenessState` und `GET /actuator/health/readinessState` antworten mit `{"status":"UP"}`, sobald der lokale Webserver erreichbar ist. `GET /ping` liefert `pong`. OpenShift und Kubernetes prüfen die Endpunkte direkt am Pod auf Port `8080`. Die Probes fragen keine externen Dienste ab. Die bisherige Variable `CONFIG_RESOURCE` hat keine Wirkung. Das Image verwendet in allen Umgebungen die produktiven Dienste von `geo.so.ch`.

Der [Docker-Workflow](.github/workflows/docker.yml) baut und testet bei Pull Requests ohne Registry-Zugangsdaten. Er veröffentlicht nach erfolgreichen Tests bei einem Push auf `main` oder einem manuellen Start auf `main`. Die Workflow-Zusammenfassung enthält Digest und Tags.

- Docker Hub benötigt im Repository `sogis/wgc-embedded` die Actions-Secrets `DOCKER_USERNAME` und `DOCKER_PASSWORD` mit Schreibzugriff auf `sogis/wgc-embedded`.
- GHCR verwendet `GITHUB_TOKEN` mit `packages: write`. Beim bestehenden [GHCR-Paket](https://github.com/orgs/sogis/packages/container/package/wgc-embedded) muss `sogis/wgc-embedded` unter **Package settings → Manage Actions access** Schreibzugriff erhalten, falls dieser noch fehlt.
- Vor dem Merge der Migration die Registry-Berechtigungen im Ziel prüfen, den [bisherigen QWC2-Publisher](https://github.com/sogis/qwc2-embedded/actions/workflows/docker.yml) deaktivieren und laufende Veröffentlichungen auslaufen lassen. Beide Publisher verwenden dieselben Tags `3`, `3.1` und `latest`. Der bisherige Java-Release-Workflow entfällt mit der Migration. Vorhandene 2.x-Tags bleiben verfügbar.

## Lokal starten

```sh
docker compose up -d --build
```

Alternativ lassen sich Build und Start direkt mit Docker ausführen.

```sh
docker build -t wgc-embedded:local .
docker run --rm -p 127.0.0.1:8080:8080 wgc-embedded:local
```

Sobald der Container lauscht, ist die Karte unter `http://localhost:8080/api/embed/v2/index.html` erreichbar. Mit `EMBED_PORT=8081` lässt sich der lokale Port wechseln. Der Container benötigt ausgehenden HTTPS-Zugriff auf `geo.so.ch`.

```html
<iframe
  title="SO!GIS-Karte"
  src="http://localhost:8080/api/embed/v2/index.html?bl=ch.so.agi.hintergrundkarte_ortho&amp;l=ch.so.agi.av.grundstuecke&amp;c=2607739,1228426&amp;s=2000"
  width="600" height="390">
</iframe>
```

## URL-Parameter

| Parameter | Funktion |
| --- | --- |
| `bl` | Hintergrundkarte `ch.so.agi.hintergrundkarte_sw`, `ch.so.agi.hintergrundkarte_farbig` oder `ch.so.agi.hintergrundkarte_ortho`. QWC2 akzeptiert auch die Kennungen ohne `ch.so.agi.`. Standard ist die graue Karte. |
| `l` | WMS-Layer von unten nach oben, durch Kommas getrennt. `layer[50]` bedeutet 50 % Transparenz. `layer!` bleibt in der URL und ist unsichtbar. Externe Layer verwenden `wms:URL#Layer`, etwa `wms%3Ahttps%3A%2F%2Fwfs.geodienste.ch%2Fav_0%2Fdeu%23Hoheitsgrenzen%5B35%5D`. Ein einzelner Query-Parameter im externen Endpunkt ist möglich. `&` ist dort auch kodiert ausgeschlossen. |
| `c` | Mittelpunkt in EPSG:2056, standardmässig `2616500,1237000`. |
| `s` | Massstabsnenner, standardmässig `378000`. Ein gesetztes `c` oder `s` wirkt unabhängig vom jeweils anderen Wert. |

QWC2 aktualisiert die URL bei Kartenbewegungen. **In geo.so.ch/map ansehen** überträgt den Ausschnitt und die Ebenen. Ein Kartenklick fragt die sichtbaren, abfragbaren SO!GIS-Ebenen ab. Im kleinen iframe sind längere Ergebnisse scrollbar. Der Proxy leitet den öffentlichen SO!GIS-Dokumentdienst formatunabhängig durch. Der Dokumentdienst entscheidet über Reports, Parameter und Auslieferungsformat.

Der historische Parameter `z` hatte im Vorgänger keine aktive Wirkung. Externe WMS müssen direkt vom Browser als WMS-1.3.0-Kartenbild für EPSG:2056 erreichbar sein. Identify und Capabilities-Anfragen entfallen für externe WMS. In einem HTTPS-iframe kann der Browser einen HTTP-Bildendpunkt auf HTTPS umstellen oder blockieren.

## Entwicklung und Tests

```sh
npm ci
npx playwright install chromium --only-shell
npm test
```

Die Tests erwarten den laufenden Container. `EMBED_ORIGIN=http://localhost:8081 npm test` wählt einen anderen Port. Für die Entwicklung ohne Docker stehen unter Node.js 22 `npm run build` und `npm start` zur Verfügung. Der Server liest den Port aus `PORT`.

Die Tests nutzen Live-Kartenbilder, dynamische Layer, den Geodienste-WMS, Identify und den Dokumentdienst. Sie prüfen zudem die fremde iframe-Origin, den Vollkartenlink, abgewiesene Proxy-Anfragen und simulierte Konfigurationsausfälle. Ausfälle der öffentlichen Dienste können Live-Tests fehlschlagen lassen.

## Integration

Der einzelne Container bündelt `qwc2-lts` mit Map und Identify. Er passt die öffentliche Konfiguration zur Laufzeit an und leitet nur die benötigten Lesepfade an `https://geo.so.ch` weiter. Browser-Cookies und Authorization-Header werden upstream verworfen. Die [Architekturentscheidung und Phasenreviews](docs/architektur.md) dokumentieren den Vergleich mit dem veröffentlichten SO!GIS-Image.

Der HTTPS-Ingress kann den vollständigen Pfad unter `/api/embed/v2/` weiterleiten. In nginx bleibt der Präfix erhalten, wenn `proxy_pass` ohne abschliessenden Slash angegeben wird.

```nginx
location ^~ /api/embed/v2/ {
    proxy_pass http://embed:8080;
}
```

Die bestehende OpenShift-Regel mit abschliessendem Slash in `proxy_pass` funktioniert ebenfalls. nginx muss dabei `X-Forwarded-Prefix` fest auf `/api/embed/v2` setzen. Der Server ergänzt den Präfix intern. Der OpenShift-Service ist auf Port 80 erreichbar und leitet an den Pod auf Port 8080 weiter.

```nginx
location ^~ /api/embed/v2/ {
    proxy_set_header X-Forwarded-Prefix /api/embed/v2;
    proxy_pass http://wgc-embedded-v2.agi-apps-test.svc/;
}
```

Die Route erlaubt fremde iframe-Origins über `frame-ancestors *`. Ein vom Ingress ergänztes `X-Frame-Options` oder restriktiveres `frame-ancestors` kann solche Einbettungen blockieren. Ein `sandbox`-Attribut auf dem iframe muss Skripte und die eigene Origin zulassen. Für den Vollkartenlink und Dokumentdownloads muss es auch Popups beziehungsweise Downloads zulassen.
