# LinuxApp — testy na telefonie

Cel: potwierdzić, że klient Wayland działa wewnątrz face i że sesje, wejście oraz procesy zachowują się poprawnie. Testy hostowe potwierdziły składnię, logikę sesji, zdarzenia protokołu i kompilację. Poniższych przypadków urządzenia jeszcze nie wykonano.

## Paczka i instalacja

Wymagany jest telefon arm64 z Androidem 10+ i **pełny** `app-embedded.apk` z runtime proot. Wersja oznaczona `compile-only` służy do sprawdzenia kompilacji i nie zapewnia uruchamiania Linuksa. Zachowaj SHA256 APK i identyfikator commita z dołączonego raportu.

1. Włącz debugowanie USB, podłącz telefon i zaakceptuj połączenie. Sprawdź `adb devices`. Przy kilku urządzeniach ustaw `ANDROID_SERIAL` na wybrany numer.
2. Zainstaluj pełną paczkę: `adb install -r app-embedded.apk`.
3. Uruchom ikonę **Linux Face Prototype** lub użyj:

   ```sh
   adb shell am start -n dev.omalauncher.embedded/dev.omalauncher.MainActivity
   ```

4. Skonfiguruj Linux w ustawieniach tej aplikacji i poczekaj na zakończenie instalacji. Wariant `dev.omalauncher.embedded` ma własne dane i własny Linux. Nie musi być domyślnym launcherem do wykonania testów.
5. Otwórz dołączoną face `linux app demo` i zainstaluj deklarowane pakiety `python` oraz `sdl2` przez mechanizm zależności widżetu.

Jeśli aktualizacja APK zgłasza `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, zatrzymaj instalację i zgłoś komunikat. Kolejne buildy CI mogą mieć inny klucz debug; nie usuwaj aplikacji z jej danymi Linuksa w ramach automatycznej procedury testowej.

## Przypadki odbioru

Dla każdego przypadku wpisz PASS/FAIL, obserwację i odnośnik do logów/nagrania. Sam widoczny proces lub otwarty port nie stanowi wyniku PASS.

| ID | Czynność | Oczekiwany wynik |
| --- | --- | --- |
| start | Dotknij demo i zaczekaj na kolorowe paski. | Obraz jest w obszarze face; nie powstaje osobne zadanie z oknem demo. |
| pointer | Aktywuj okno drugim dotknięciem, dotknij pasków. | Kolory zmieniają się, sąsiednie elementy OWL pozostają dostępne. |
| keyboard | Użyj ⌨ i klawisza spacji; następnie klawiatury sprzętowej, jeśli dostępna. | Klawisze trafiają do aktywnego klienta, zmieniając kolory. Escape kończy demo. |
| focus | W czasie dotyku/klawisza użyj Back, dotknięcia poza oknem, zmiany strony i przełączenia aplikacji. | Gest jest zakończony, nic nie pozostaje wciśnięte i następne okno nie otrzymuje starego wejścia. |
| lock | Trzymaj dotyk lub przycisk myszy podczas blokowania telefonu; odblokuj. | Po powrocie wejście działa od nowa, bez utkniętego gestu. |
| resize | Obróć telefon i otwórz/zamknij klawiaturę ekranową. | Obraz dopasowuje rozmiar; współrzędne dotyku odpowiadają obrazowi. |
| stop | Zatrzymaj demo przez ×; uruchom ponownie. Powtórz podczas startu. | Stary proces i jego potomkowie kończą się; nowe uruchomienie jest niezależne. |
| instances | Utwórz dwa LinuxApp z automatycznymi ID, także w pętli z trwałym key oraz w dwóch użyciach komponentu. Zmień kolejność. | Każde okno ma własną sesję; zmiana kolejności zachowuje poprawne przypisanie. |
| identity | Użyj duplikatu jawnego appId oraz klienta z niedopasowanym ID. | Duplikat otrzymuje błąd; przy niedopasowaniu węzeł pokazuje oczekiwane ID i pozostaje możliwy do zatrzymania. |
| lifecycle | Zmień command/appId podczas startu, usuń cały widżet i dodaj go ponownie. | Anulowany start nie otwiera spóźnionego zwykłego zadania; procesy usuniętego widżetu kończą się. |
| errors | Uruchom nieistniejący program i spróbuj ponownie z poprawnym command. | Jest czytelny błąd oraz możliwość ponownego startu; launcher pozostaje sprawny. |
| regression | Uruchom istniejący Terminal, klienta foot i popup zgodnego klienta Wayland. | Terminal nadal działa; popup jest poprawnie wyświetlany i przyjmuje wejście. |

Ustalona polityka: zejście ze strony, ukrycie warunkowego węzła lub usunięcie elementu listy zachowuje jego sesję do usunięcia/przeładowania całego widżetu albo zmiany polecenia przy ponownym renderowaniu. Użyj × przed ukryciem programu, który ma się zakończyć. Brak zakończenia procesu po 3 sekundach ma być pokazany jako niedokończone zatrzymanie; testu takiego przypadku nie oznaczaj PASS dla skutecznego zatrzymania. Hostowe testy nie potwierdzają zachowania drzewa procesów proot na Androidzie.

## Zbieranie wyników

Po odtworzeniu przypadku, gdy aplikacja nadal działa:

```sh
bash prototype/collect-linuxapp-logs.sh focus
```

Zamiast `focus` podaj ID z tabeli. Skrypt odczytuje model/wersję Androida, wersję aplikacji oraz logcat procesu `dev.omalauncher.embedded`, bez czyszczenia buforów i danych aplikacji. Wybór urządzenia odbywa się standardowo przez `ANDROID_SERIAL`. Wynik trafia do katalogu `diagnostics/linuxapp-…`; można zmienić jego rodzica zmienną `LINUXAPP_DIAGNOSTICS_DIR`.

Dołącz nagranie z systemowego rejestratora ekranu, krótki opis oczekiwanego i zaobserwowanego zachowania, identyfikator commita oraz SHA256 APK. Jeśli proces uległ awarii i już nie działa, zachowaj komunikat skryptu i zgłoś ten fakt; logi bieżącego PID nie obejmują poprzedniego, zakończonego procesu.
