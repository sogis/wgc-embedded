# Architekturprüfung E0

Referenzen: wgc-embedded `42baa98`, sogis-qwc2-app (2026-lts) `fc0e440`, qgis/qwc2 (2026-lts) `8a1eb79`, qwc2-e2e-tests `15ccedb`. Der Kartenkatalog bezieht Konfiguration, Ebenendefinitionen und Kartendienste live von geo.so.ch. Dessen feste Upstream-Adressen, anonymen Requests und explizite Endpunktfreigabe genügen auch hier. Der Java-Katalog, die Suche und der Entwicklungsproxy entfallen.

## Machbarkeitsnachweis und Varianten

- **A, fertiges Image.** Registry-Tags `v2026.13-lts` bis `v2026.15-lts` sind Linux/amd64. Für Chromium wurden die Webdateien aus `sourcepole/sogis-map-viewer:v2026.15-lts@sha256:81baa4ed02cf3a8379d4b3d171b8699c95ac381db5278699242cacaed3861ae5` unter `/qwc2` extrahiert. Sie enthalten `assets`, `dist`, `translations`, HTML und Basiskonfiguration. Das Original-HTML startet Matomo. Eine eigene HTML-Seite lieferte die Assets unter dem Embed-Pfad mit Live-Konfiguration und nur Map/Identify aus. Die vom Bootstrap vorgesehene Markierung `config:autologin=1` übersprang `/auth/info` und wurde anschliessend entfernt. Chromium lud Orthofoto-Kacheln, das dynamische WMS der Erdwärmesonden und dessen HTML-Resultat. Grundstücks-Identify erzeugte aus `featurereport` einen Objektblatt-Link.
- **Grenze von A.** Mit dem externen WMS aus dem Alt-README stellte Chromium erfolgreich `GetMap` dar. Beim Klick stellte derselbe Build zudem eine externe `GetFeatureInfo`-Anfrage und übernahm deren Ergebnis in die Identify-Ansicht. Der Stock-Restorer übernimmt `queryable` aus Capabilities. Identify hat gemäss Quellcode keine Einstellung, um externe Ebenen auszuschliessen. Der Restorer setzt ausserdem lesbare Capabilities (XHR/CORS) voraus, während der Vorgänger externe WMS direkt als Bildquelle verwendet. Die URL-Rückgabe im Prototyp ergänzte beim externen Endpunkt `?&`. Ein Umbau der externen Ebenen als künstliche Theme-Layer oder Manipulation des fertigen Bundles würde über eine kleine Konfigurationsanpassung hinausgehen.
- **B, Minimal-Build.** Vorschlag für das Gate. `qwc2-lts@2026.0.30` entspricht der Abhängigkeit der SO!GIS-Referenz. Ein kleiner Bootstrap registriert Map und Identify sowie den vorhandenen SO!GIS-Restorer. Der QWC2-Hook `externalLayerRestorer` liefert aus `wms:URL#Layer` eine normale, nicht abfragbare QWC2-WMS-Definition an die vorhandene Ebenenverwaltung. Das erhält bildbasierte WMS-Anfragen ohne Capabilities. Es setzt weder Änderungen am QWC2-Code noch einen allgemeinen WMS-Proxy voraus. Die HTML-Einstiegsseite enthält den Vollkartenlink. Ein Node-Server liefert gebaute Dateien und adaptiert die öffentliche Live-Konfiguration. Die erlaubten SO!GIS-Lesedienste werden unter dem Embed-Präfix anonym weitergeleitet.
- **Grösse.** Das Image startet mit 5'643'689 Byte JavaScript (gzip lokal 1'549'569 Byte). Die drei Hauptdateien aus dem alten Viewer messen zusammen 1'052'540 Byte unkomprimiert. Einschliesslich des vom Loader geladenen GWT-Moduls sind es 1'102'953 Byte. Die Grösse ist kein eigenständiges Ausschlusskriterium für A.

## Paritätsmatrix

| Status im Vorgänger | Verhalten | Zielprüfung |
| --- | --- | --- |
| Dokumentiert und implementiert | Öffentlicher Einstieg `/api/embed/v2/index.html`, drei WMTS-Karten `sw`, `farbig`, `ortho` mit Titeln `ch.so.agi.hintergrundkarte_*` | Dieselbe Browseradresse, alle drei Kachelsätze. QWC2 verwendet die kurze Kennung. |
| Dokumentiert und implementiert | `l`: Listeneinträge werden unten nach oben gezeichnet, `[n]` ist Prozent Transparenz, `!` blendet aus | Reihenfolge in WMS und URL, tatsächliche Transparenz, ausgeblendete Ebene bleibt in URL. |
| Dokumentiert und implementiert | `wms:URL#Layer` lädt direkt ein WMS-1.3.0-Bild. Kein Identify. `&` im externen Endpunkt ist ausgeschlossen. | Reale Antwort des README-WMS, direkter Bildrequest, kein externer Identify, unveränderte URL nach Bewegung. |
| Dokumentiert und implementiert | `c` und `s` sind unabhängig. Zentrum 2616500,1237000 und Auflösung 100 m/px sind Defaults. `ViewManager` rechnet mit 3780 px/m. | Start, beide Kombinationen einzelner Werte, beide Werte gemeinsam, sowie einen Zwischenmassstab anhand realer WMS-BBOX prüfen. |
| Implementiert und produktiv beobachtet | URL-Aktualisierung bei Kartenbewegung, Zustandsübernahme für Vollkarte | Pan/Zoom und tatsächlich geöffneten Link im Browser prüfen. |
| Implementiert | Sichtbare interne Layer bei Klick, mehrere HTML-Ergebnisse und Objektblatt per `featurereport` | Dynamisches Grundstückslayer, Layerüberschrift, HTML-Inhalt, PDF-Ziel, schliessen, kleines iframe. |
| Implementiert, ohne aktive Auswirkung | Parser liest `z`, aber MapComponent hat keinen aktiven Subscriber | `z` ist historisch. |
| Historisch/auskommentiert | `z`-Schreiblogik sowie früherer Link mit Favicon-Ressource | Aktuellen Textlink verwenden. |
| Bekannte Einschränkung | Ohne `bl` blieb die alte produktive Karte leer. Der Vollkartenlink konnte ohne initiale Kartenbewegung auf `undefined` zeigen. | Graue Live-Standardkarte und von Beginn an verwendbarer Vollkartenlink. |
| Bekannte Einschränkung | Identify des Vorgängers zählt nur `HtmlContent`; Live-Format und Layout der XML-Antwort haben sich geändert. | Reale HTML-Antwort durch QWC2 anzeigen und Layernamen/Reports prüfen. |

Die alten `config*.json`-Varianten wechseln WMS/WMTS/Identify/Dokument- und Vollkarten-Hosts zwischen den Umgebungen. Dieses Projekt nutzt die produktiven öffentlichen Dienste. Das Default-Thema enthält derzeit keine fachlichen Sublayer. Der Solothurn-Restorer liest sie bei Bedarf über `weblayers?filter=…`.

## Phasenreviews

Die folgenden Reviews liefen unabhängig mit OpenCode 2.0.28 und `deepseek/deepseek-flash`. Die angegebenen Commits liegen im Quell-Repository [sogis/qwc2-embedded](https://github.com/sogis/qwc2-embedded).

| Phase | Commit | Ergebnis |
| --- | --- | --- |
| E0 | `1cd60a663847f015f61dcde83c4535754d96ca3c` | **PASS.** Die unabhängigen Browserproben bestätigten externe Identify-Anfragen und die veränderte externe URL beim fertigen Image. Der Review verlangte eine feste Proxy-Allowlist, den Callback mit der Platzhalter-ID und die Legacy-Startwerte im Bootstrap. |
| E1 | `7a5cd34ef8c1802666d40d214096c9cd6e612382` | **PASS.** Eigenständiger Build, Live-Kartenbilder und die Proxy-Grenzen verifiziert. Der Review hielt den Browser-Direktzugriff auf den Dokumentdienst zur Korrektur in E3 fest sowie die URL- und Browser-Kompatibilität für E2. |
| E2 | `2bd787827f5290465207d325f29ac699fde883f6` | **PASS.** Der unabhängige Browserdurchlauf mass für `s=4347` 1,1501 m/Pixel, auch nach Reload, und bestätigte den Vollkartenlink. |
| E3 | `5d74f3f2686c8636eb64d284732a2ae2bb3f217d` | **PASS.** Im 265 × 174 px grossen iframe bestätigte der Review zwei native HTML-Ergebnisse, gültige PDF-Daten und elf Negativfälle der Dokumentroute. |
| E4 | `9df69335b1079d6f4b1dfa20b5b0d4a6553cce45` | **PASS.** Separater Compose-Betrieb, 7/7 Tests, gültige CSP und anonymer Identify-Aufruf aus einem fremden Origin. |
| E5 | `7b2d88c94b481e291aea0a9a1e9beb054384b6da` | **PASS.** Unabhängiger Gesamt- und Vereinfachungsreview gegen `main`, 8/8 Tests mit separatem Compose-Projekt und zusätzliche Live-WMS-Gegenprobe. |
| Vollkartenlink | `c0c5975812541d8f1db90d8ea9ade1301bb446cc` | **PASS.** Unabhängiger Nachreview mit Chromium bei 180 und 265 px Breite. Lokal 8/8 Compose-Tests, einschliesslich Identify und Vollkartenlink. |
| Dokument-Proxy | `6536b7b0e38dbda21e5e89b6c0f1c134904a2ea3` | **PASS.** Unabhängiger Review der technischen Pfadgrenze und des Durchreichens. Lokal 9/9 Compose-Tests mit Live-PDF und kontrollierter XLSX-Antwort. |

Der Zoombereich folgt den Live-WMTS-Auflösungen, die mit dem alten Kartenviewer übereinstimmen. Ein erforderlicher Zwischenmassstab wird als zusätzlicher Startwert eingefügt. QWC2 interpoliert Bruchzooms linear, OpenLayers logarithmisch. Deshalb korrigiert der Bootstrap den Wert `s` in der bestehenden QWC2-URL-Warteschlange anhand der tatsächlich angezeigten Auflösung. Der Vollkartenlink liest diese Auflösung und den Kartenmittelpunkt direkt bei Aktivierung, auch vor der verzögerten URL-Aktualisierung.

QWC2 Identify zeigt die vom öffentlichen SO!GIS-FeatureInfo gelieferte HTML-Tabelle für die Bohrtiefenabfrage und das Grundstück mit den produktiven Layerbezeichnungen. Der Vorgänger verlinkte `featurereport` ohne Dateiendung. QWC2 ergänzt `.pdf`. Beide Formen lieferte der Live-Dienst beim geprüften Grundstück. Der Embed-Proxy gibt Unterpfad und Query des Dokumentdiensts ohne fachliche Auswertung weiter. Er übernimmt Status, Content-Type und gegebenenfalls Content-Disposition. Der Dokumentdienst bestimmt die verfügbaren Reports, Parameter und Formate. Eine CSP-Sandbox begrenzt aktive Inhalte bei Inline-Anzeige unter dem Viewer-Origin. Der zusätzliche Fachlink im HTML zur Bohrtiefenabfrage zeigt weiterhin auf den vom Dienst gelieferten dox42-Endpunkt.

## Messungen und Integrationsproben

Der Minimal-Build umfasst 2'565'264 Byte JavaScript (771'013 Byte bei lokaler gzip-Komprimierung) und 63'409 Byte CSS. Der Node-Container liefert die Dateien lokal unkomprimiert aus. Drei Chromium-Starts in neuen Browser-Kontexten gegen Docker Compose benötigten vom Seitenaufruf bis zum ersten sichtbaren Orthofoto-Pixel 1,51 s, 1,28 s und 1,31 s. Der erste WMS-Response war nach 0,83 s, 0,66 s und 0,69 s eingetroffen. Die Werte enthalten Live-Netzwerkzugriffe und sind keine Produktionsmessung.

Die automatisierte Suite prüft einen iframe auf einem separaten lokalen HTTP-Origin. In einer manuellen Chromium-Gegenprobe über zwei lokale HTTPS-Origins mit einem temporären Zertifikat erschien derselbe externe Geodienste-WMS als direktes `GetMap`-Bild. Eine `http:`-Referenz auf diesen WMS stufte Chromium auf HTTPS hoch. Das Browser-Verhalten lässt sich für einen HTTP-only-Dienst nicht zusichern. Weiter bestätigte eine instrumentierte Live-Anfrage auf der Node-Grenze, dass eingehende synthetische `Cookie`- und `Authorization`-Header bei Config, Themes und weblayers im Upstream-Request fehlten.

## Abschliessende Vereinfachung

Der Bootstrap registriert die zwei QWC2-Plugins und ergänzt ihre bestehenden Hooks für Live-Layer, WMS-Endpunkte und den Startmassstab. Die URL-Warteschlange, Kartendarstellung, Ergebnisliste und Downloads laufen über QWC2 beziehungsweise den Browser. Der Vollkartenlink bleibt ein HTML-Link. Der Proxy verwendet ausschliesslich Node.js-Bordmittel. Die direkte `lodash.isempty`-Abhängigkeit entfiel, da QWC2 sie selbst deklariert.

Der gebaute Stand umfasst 2'565'447 Byte JavaScript (771'096 Byte mit gzip). Ein Browser-Test zeigt, dass ein einzelner Query-Parameter eines externen WMS in ursprünglicher Schreibweise ankommt. Eine gesonderte Probe mit dem SO!GIS-Identify-XML und simuliertem `inline="0"` bestätigte natives HTML in einem QWC2-Ergebnisiframe unter der CSP. `frame-src about:` ermöglicht diesen Anzeigepfad.

Eine abschliessende manuelle Chromium-Probe öffnete den produktiven Vollkartenlink mit Orthofoto, Grundstückslayer, Mittelpunkt `2607739,1228426` und `s=2000`. Das Ziel lud WMTS und Grundstücks-WMS mit HTTP 200 und dem übertragenen Mittelpunkt. Die Vollkarte verwendet ihre eigenen Zoomstufen.
