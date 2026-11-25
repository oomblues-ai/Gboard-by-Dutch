# Local AI met Gboard gebruiken

Local AI voegt een optionele lokale route toe voor schrijfhulp, beeldtekst en dicteren. De afzonderlijke app Local Model Host beheert de modellen, zodat Gboard en andere integrerende apps ze delen. Voor installatie en inferentie zijn Play Store, AICore, een Google-account en Google Play-services niet nodig.

De oorspronkelijke Gboard-backends blijven beschikbaar. Dit is geen schakelaar die al het Google-verkeer uit het toetsenbord verwijdert: GIF-zoeken, synchronisatie en bewust gekozen Google-functies houden hun eigen gedrag.

## De companion installeren

1. Pas **AI Writing Tools**, **Enable OCR / Scan Text** en voor gedeeld dicteren **Parakeet voice typing** toe in Morphe. Behoud bij updates dezelfde pakketnaam en ondertekeningssleutel.
2. Open **Patches → AI & Voice → Local AI** in het gepatchte Gboard. Kies **Get Local Model Host** of **Update Local Model Host**. Gboard downloadt en verifieert de vastgelegde ondertekende APK voordat Androids installatievenster opent.
3. Sta installatie vanuit Gboard toe als Android dat vraagt. Behoud bij updates de gegevens van de bestaande companion.
4. Open **Model setup**. Kies Gemma 4 E2B of het optionele E4B-profiel als jouw hardware dat aangeboden krijgt. Download het gekozen model en schakel Local AI en tekst-/beeldverwerking in. Zet beeldondersteuning aan voor beeldtaken.
5. Wacht tot het tekst-/beeldmodel **Ready** meldt. Ga terug naar Gboard en zet **Use Local AI** aan.

De huidige companion vereist **Android 11 of nieuwer en 64-bits ARM**. E4B heeft meer geheugen nodig dan E2B. Een selecteerbaar model is geen garantie dat het onder de huidige omstandigheden kan laden. De instellingen onderscheiden downloaden, verifiëren, laden, Gereed en een geheugenpauze.

Gebruik voor installatie zonder Gboard het [companion-project](https://gitlab.com/johninnld/local-model-host) en de [geverifieerde installatieprocedure](../developers/local-ai/install.md).

## Schrijven in het toetsenbordthema

Open **Writing Tools** via de werkbalk. Lokale acties volgen het toegepaste toetsenbordthema. Probeer Proofread, Rephrase of Professional op een korte tekst, controleer het voorbeeld en pas toe of annuleer. Behoud het gewone ongedaan maken van de editor.

Waar aangeboden zijn er ook verkorten, samenvatten, antwoorden, stijlvoorbeelden en een opgeslagen instructie. Een antwoord heeft aangeleverde gesprekscontext nodig; een andere app wordt niet automatisch uitgelezen. Gebruik **Read this screen** expliciet en bevestig Androids schermopnametoestemming. Apps die opname blokkeren kunnen zo niet worden gelezen. Er wordt geen bericht namens jou verstuurd.

**AI Translate** gebruikt de doeltaal onder **Writing preferences → Translation language**. Kies die eerst. Experimentele Smart Compose en inline grammatica hebben eigen schakelaars onder Writing preferences; controleer suggesties nog steeds.

## Tekst uit een afbeelding lezen

Met Local AI geselecteerd en vision Gereed open je **Scan Text** en kies je een afbeelding of de cameraoptie. Controleer de herkende tekst vóór selectie of invoegen. Een foto of schermafbeelding kan gevoelige informatie bevatten; kies alleen het bedoelde beeld.

De lokale beeldroute geeft gegenereerde herkende tekst terug. Controleer namen en getallen. Dit is iets anders dan barcode-/QR-scannen en levert geen OCR-woordvakken of zekerheidscores. De bestaande Google/gebundelde OCR-route heeft eigen instellingen wanneer Local AI niet geselecteerd is.

## Lokaal dicteren instellen

Open **Voice typing → Local AI voice typing → Open or install Local AI**. Kies en download een spraakprofiel onder **Local AI → Speech recognition**, schakel spraak in en wacht op Ready. Zet daarna **Use shared local speech** in Gboard aan.

| Keuze | Wat controleren |
| --- | --- |
| Parakeet TDT v3 of Ultra | Opgegeven talen en download van het geselecteerde profiel |
| Nemotron Streaming | Taal- en gereedheidsgegevens van het streamingprofiel |
| Whisper Tiny | De meegeleverde integratie gebruikt een Engelse prefix |

Downloads staan in de companion, niet afzonderlijk in Gboard. Tekst-/beeldverwerking mag uit staan als je alleen spraak wilt. Microfoon- en invoeginterface blijven die van Gboard. Geselecteerde lokale spraak meldt een ontbrekend model of geheugenpauze en schakelt niet stilzwijgend naar Google.

De gedeelde adapters hebben niet de akoestische spraakwoordenboekvoorkeur van de oude ingebouwde Parakeet-decoder. Google/Rambler-instellingen gelden voor die gekozen backends; ze geven niet aan elke lokale herkenner dezelfde opdrachten of woordenboekwerking.

## Geheugen, opslag en privacy

**Always ready** houdt een voorgrondmelding actief en verkort koud starten. On-demand bespaart belading tussen taken. Android kan een proces nog steeds opruimen en echte geheugendruk kan laden pauzeren. Een client-app kan jouw Stop niet opheffen.

Alleen geselecteerde beheerde modeldownloads blijven bewaard. Een wissel verwijdert de vorige beheerde download nadat actief gebruik is afgelopen. Originele importbestanden in Downloads beheer je zelf.

Het schrijfbeleid heeft een basisset van 38 talen. Extra talen blijven beschikbaar met een duidelijke waarschuwing over experimentele kwaliteit; kwaliteit en snelheid verschillen per taak en model. Spraak- en stemproviders hebben eigen taallijsten.

Tekst-, beeld- en spraakinferentie blijven op het toestel. Voor APK- en modelinstellingen zijn downloads nodig. Search tools gebruiken het netwerk alleen als jij ze aanzet en de client-app ze toestaat. Controleer iedere gegenereerde bewerking vóór toepassing.

[Alle Gboard-patches](gboard.md) · [Integratie voor ontwikkelaars](../developers/local-ai/index.md)
