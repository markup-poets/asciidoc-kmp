# Minimaler AsciiDoc-Konverter in Kotlin Multiplatform: Architektur und Design

## Einleitung
AsciiDoc ist eine umfangreiche, semantische Markup-Sprache für technische Dokumentation. Sie ermöglicht es, aus einer einfach zu lesenden Textdatei verschiedenste Ausgabeformate zu erzeugen (HTML, PDF, ePub, Man-Pages, etc.).

Ein AsciiDoc-Prozessor liest ein AsciiDoc-Dokument, erstellt daraus ein internes Dokumentenmodell und generiert aus diesem Modell das gewünschte Ausgabeformat.

Kotlin Multiplatform (KMP) erlaubt die Entwicklung einer einzigen Codebasis für JVM, JavaScript und native Plattformen. Ein AsciiDoc-Konverter in KMP kann daher plattformübergreifend eingesetzt werden – z. B. als CLI-Tool, IDE-Plugin oder Web-Anwendung.

Ziel ist ein:
- plattformunabhängiger Konverter  
- spec-konformer AsciiDoc-Support (TCK-kompatibel)  
- klar strukturierte Pipeline (Parse → Process → Convert → Render)  
- modularer und erweiterbarer Aufbau  
- keine externen Abhängigkeiten  

---

## AsciiDoc-Spezifikation und TCK
Die AsciiDoc Language Specification wird unter der Eclipse Foundation entwickelt. Sie definiert Syntax, Grammatik und APIs der Sprache.

Ein zentrales Element ist das **Technology Compatibility Kit (TCK)**, das sicherstellt, dass Implementierungen korrekt und kompatibel sind. Solange die Spezifikation im Draft-Stadium ist, gilt **Asciidoctor** als Referenzimplementierung.

---

## Architektur und Pipeline-Design
Die Verarbeitung erfolgt in klar getrennten Phasen:

1. **Parse** – Analyse des AsciiDoc-Quelltexts → AST/ASG  
2. **Process** – Auflösen von Includes, Attributen, Substitutionen  
3. **Convert** – Transformation in ein Zielformat (z. B. HTML)  
4. **Render** – Finale Ausgabe oder Persistenz  

Diese Trennung erhöht Wartbarkeit, Testbarkeit und Erweiterbarkeit.

---

## Parse-Phase
Der Parser erzeugt ein plattformneutrales Dokumentmodell (AST).

Merkmale:
- zeilenbasierte Verarbeitung
- Zustandsmaschinen für komplexe Strukturen
- separate Inline-Markup-Verarbeitung
- keine externen Parser-Bibliotheken

Beispiele für AST-Knoten:
- Document
- Section
- Paragraph
- List / ListItem
- InlineText, Strong, Emphasis

---

## Process-Phase
Nach dem Parsen wird der AST weiter angereichert:

- Includes auflösen (`include::file[]`)
- Attribute ersetzen (`{version}`)
- Makros und Substitutionen
- Validierung und Normalisierung
- automatische Inhalte (z. B. TOC)

Die Phase ist erweiterbar über **DocumentProcessor**-Interfaces.

---

## Convert-Phase
Der Converter wandelt den AST in ein Zielformat.

Beispiel-Interface:
```kotlin
interface Converter<Output> {
    fun convert(document: Document): Output
}
```

Typische Implementierungen:
- HTML
- Plain Text
- Debug-/AST-Dump

Traversal erfolgt meist per Visitor-Pattern oder `when`-Dispatch.

---

## Render-Phase
Der Renderer übernimmt die finale Ausgabe:
- Schreiben in Dateien
- Rückgabe an IDE/UI
- optionale Format-Nachbearbeitung (Templates, Styles)

Auch hier bleibt alles austauschbar und plattformneutral.

---

## Modularität und Erweiterbarkeit
Empfohlene Modulstruktur:
- `asciidoc-core`
- `asciidoc-html`
- `asciidoc-cli`
- `asciidoc-extensions`

Erweiterungspunkte:
- eigene Block- und Inline-Syntax
- Treeprocessor
- zusätzliche Converter
- benutzerdefinierte Output-Targets

---

## Beispiel-Pipeline (Kotlin)
```kotlin
val parser = AsciidocParser()
val ast = parser.parse(input)

val processor = AsciidocProcessorPipeline()
processor.addProcessor(TodoBlockProcessor())
processor.process(ast)

val html = HtmlConverter().convert(ast)
HtmlRenderer().render(html, outputFile)
```

---

## Fazit
Ein minimaler AsciiDoc-Konverter in Kotlin Multiplatform kann leichtgewichtig, spec-konform und hochgradig erweiterbar sein. Durch eine saubere Pipeline-Architektur lassen sich komplexe Anforderungen elegant umsetzen – ohne externe Abhängigkeiten.

Kotlin Multiplatform ermöglicht dabei maximale Wiederverwendbarkeit über alle relevanten Plattformen hinweg.
