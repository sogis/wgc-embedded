# Arbeitsregeln

- Wende Ockhams Rasiermesser, KISS und YAGNI an. Bevorzuge direkte Architektur und Standardmechanismen vor generischen Lösungen und Eigenentwicklungen.
- Arbeite innerhalb des vereinbarten Umfangs autonom. Ändere keine Referenzrepositories und führe keine unaufgeforderten Deployments oder Merges durch.
- Teste jedes relevante Verhalten über seinen normalen Einstiegspunkt. Dokumentiere tatsächliche Entscheidungen, Prüfungen und Einschränkungen.
- Verwende `simplify-change` vor der Implementierung und nach grösseren Änderungen. Lass jede Phase mit `review-change` und `simplify-change` unabhängig per OpenCode 2.x und `deepseek/deepseek-flash` prüfen. Halte Phase, Commit und Ergebnis fest. Bei fehlender Review-Infrastruktur ist das Gate BLOCKED.
