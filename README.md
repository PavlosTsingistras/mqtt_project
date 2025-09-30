# Ανάπτυξη Λογισμικού για Δίκτυα και Τηλεπικοινωνίες

**XXXX XXXXX** `sdiXXXXXX`  
**Charalmpos Tsitsiringos** `sdi1900198`  
**Παύλος Τσιγγίστρας**  `sdi2000264`




## Γενική εικόνα του Project

### Τεχνολογίες
- Spring Boot 3
- Java 17
- ReactJS
- Node JS 22 (προτείνουμε nvm)
- Android Native με Java+XML
- Docker για deployment
- Paho MQTT Java client
### Σημαντικό: Λόγω έλλειψης support στο Android Client της Paho για Android 12+ συσκευές , χρησιμοποιήσαμε το Java Client και στα Android Apps. Λειτουργεί

### Δομή Repo
Θα δείτε στο main branch φακέλους για κάθε μέρος του app:
- `app` - ο κώδικας για την android συσκευή (χρήστης)
- `Iot` - με 2 apps το καθένα με διαφορετικα hardcoded topics και ids
    - IotDevice1App
    - IotDevice2App
- `edge_server` - ο κώδικας για τον springboot
- `map` - το react application για την οπτικοποίηση των δεδομένων
- `docker` - υπεύθυνος για το configuration και deployment των:
    - Mysql server
    - Mosquitto Server
    - Edge Server με την χρηση Dockerfile
#### Docker troubleshooting:
- `"file not found "` - δεν τρέχει ο docker daemon

- Άμα κρασάρει για κάποιο άλλο λόγο μπορεί να προσπαθεί να ανοίξει κάποιο server σε port που ειναι **occupied** από κάποιο άλλο πρόγραμμα του υπολογιστή σας


## Οδηγίες εκτέλεσης

### Βήμα 0:
Εγκατάσταση docker και docker compose. Java 17 SDK και android studio με AGP 8.7.3> support. Node version 22LTS. Διαθέσιμα όλα αυτά σε windows και linux.

### Βήμα 1:
Navigate στον docker φάκελο και εκτελέστε την εντολή:

```docker compose up --build```


Μπορεί να πάρει λίγη ώρα αυτή η εκτέλεση καθώς κατεβάζει dependencies και κάνει build τον edge_server.
Εν τέλη ανοίγει τα παρακάτω ports:
- Edge Server: Port 8080 (websockets)
- Mosquitto Server: Port 1883
- MySQL Server: Port 3306

Μέσα στο docker-compose.yaml file μπορείτε να δείτε τους κωδικούς της βάσης, καθώς και άλλα config parameters.

### Βήμα 2:
Ανοίγετε android studio και κάνετε navigate στο φάκελο iot. Κάνετε build και τα 2 apps είτε σε ξεχωριστα emulators είτε σε ξεχωριστές συσκευές. Βάζετε την ip σας και το port όταν σας ζητηθεί στα app.

### Βήμα 3:
Κάνετε navigate στο app directory και κάνετε build με android studio το android app (user)

### Βήμα 4:
Κάνετε navigate στο map directory. Εκτελέιτε τις εντολές

`npm i` &

`npm start`

Αυτό θα ανοίξει την ιστοσελίδα στο port 3000 (default)

## Λεπτομερείς αναλύσεις

### Edge Server

#### Γενική Υλοποίηση
- Mqtt Connection και websockets (port 8080)
- Services για Risk Evaluation, DeviceState, Notification και MQTT Setup/Initialization
- Models για Sensors, Android Location, Risk Events (με JPA ORM για την MySQL βάση), Android Alerts με σκοπό την robust ενδοεπικοινωνία των connected devices
- Χρήση lombok για ελαχιστοποίηση boilerplate code και SLF4J για logging

#### Mqtt / Websocket flow
1. Λαμβάνουμε ένα μήνυμα
2. Αν είναι από το android device τότε κάνουμε save στο lastKnowLocation την τοποθεσία και στέλνουμε με websocket την νέα τοποθεσία στο map
3. Αν είναι από κάποιο iot device, τότε επεξεργαζόμαστε τα δεδομένα και δημιουργούμε τα κατάλληλα αντικείμενα. Ελέγχουμε αν υπάρχει risk και κάνουμε update το device state instance. Αν υπάρχει risk τότε στέλνουμε το κατάλληλο alert στο mqtt topic για τα alerts και κάνουμε save στην βάση το risk event.

- Ο υπολογισμός της απόστασης γίνεται με την [Haversine formula](https://en.wikipedia.org/wiki/Haversine_formula)
- Η υλοποίηση των websockets έγινε με ενα [CopyOnWriteArray](https://stackoverflow.com/questions/2950871/how-can-copyonwritearraylist-be-thread-safe) καθώς έτσι αποφεύγουμε ταυτόχρονη αλλαγή από διαφορετικά threads, κάτι που οδηγεί σε exceptions. Κάπως σαν σημαφόρους δηλαδή (/ mutex).

## Android App

### Λειτουργικότητα MQTT
- Subscribe στο topic `android/notifications`
- Publish στο topic `android/location`
- Ζητάει permissions για location
- 2 κουμπιά για έναρξη/διακοπή του location transmission
- Mqtt Config Panel για δήλωση ip και porto
- Transmission duration config. Με κάθε αλλαγή από manual σε automatic κάνει reset ξανά σε continuous transmission
- Exit app functionality
### LocationService
Η κλάση `LocationService` είναι υπεύθυνη για:
- Επιστροφή των lat/lng τιμών:
    - Manual mode
    - Automatic mode
- Περιοδικός έλεγχος internet connection (κάθε 10 sec)

Για το manual mode χρησιμοποιείται η κλάση `XmlParser` που περιέχει το logic για:
- Ανάγνωση δεδομένων από xml files
- Τυχαία επιλογή αρχείων στο MainActivity

### MainActivity
- Ζήτηση location permissions
- Αρχικοποίηση MqttConnection
- Χειρισμός εισερχόμενων μηνυμάτων:
    - Λήψη από το subscribed topic
    - Κλήση της `handleAlert` function για render του alert μέσω callback
## IoT Device App

### Γενική Επισκόπηση
Υπάρχουν 2 διαφορετικά apps με τις εξής διαφορές:
- Publish σε διαφορετικά topics: `topic/{deviceId}` (όπου deviceId = 1 ή 2)
- Διαφορετικά ονόματα στο `build.gradle`
- Διαφορετικά `MQTT_CLIENT_ID`
- Διαφορετικό `deviceId` στο JSON payload


### MqttManager
Κλάση που επεκτείνει τον MQTT Java client με custom υλοποιήσεις , σεταρει το settings panel και παρεχει mqtt config options με ip και port

#### Connection Handling
Υπάρχει 2 second connection timeout που αν δεν έχει καταφέρει να συνδεθεί πετάει error και ενημερώνει τον χρήστη
```
MqttConnectOptions options = new MqttConnectOptions();
options.setConnectionTimeout(2); // 2 sec timeout 
```


### Sensor Management

#### SensorSliderFragment
**Υπευθυνότητες:**
- Διαχείριση λίστας sensors
- Dynamic UI generation για sensor sliders
- Rendering των sensor controls

#### JSON Payload Creation
Με την κλαση `createJsonPayload` φτιάχνουμε το json που θα στείλουμε στον server


### Settings Panel
**Λειτουργίες:**
- Δημιουργία νέων sensors
- Προσθήκη sensors στη λίστα
- Μεταφορά sensor data στο fragment

### Εφαρμογή Map

Είναι υπεύθυνο για την οπτικοποίηση διαφόρων οντοτήτων εντός της εφαρμογής, όπως συσκευών android (αναφερόμενη ως συσκευή χρήστη) και των συσκευών iot (αναφερόμενα ως sensors), καθώς και των καταστάσεων στις οποίες βρίσκονται οι συσκευές αυτές.

Αρχικά, μια σημαντική ιδιότητα της εφαρμογής είναι ότι συνδέεται με τον edge server μέσω websockets. Συγκεκριμένα, διαχειρίζεται 2 συνδέσεις:
- μια για την android συσκευή
- μια για τις iot συσκευές

Δέχεται σε αυτά τα sockets, streams από δεδομένα σε φόρμα JSON (λαμβάνει την γεωγραφική τοποθεσία, τις καταστάσεις και τιμές που κατέχουν οι αισθητήρες, το id της εκάστοτε συσκευής, αν βρίσκεται σε κρίσιμη κατάσταση κτλ.).

Στη συνέχεια δημιουργήθηκε μια κλάση `SensorData`, η οποία δημιουργία instances για τα δεδομένα των αισθητήρων και τις καταστάσεις τους. Λαμβάνοντας τα δεδομένα των streams των iot συσκευών μοντελοποιεί μέσω του constructor, τα instances καθώς και τις τιμές των μεταβλητών κατάστασης, όπως `isDis` που δείχνει αν είναι οι αισθητήρες μιας συσκευής είναι όλοι ανενεργοί.

Αντίστοιχα υπάρχει και ακόμα μια κλάση για την δημιουργία object android συσκευών. Διαχειρίζεται την γεωγραφική τοποθεσία μιας συσκευής.

Τέλος οι παραπάνω λειτουργίες, συνδιάζονται στο βασικό πρόγραμμα `App.js`. Στο οποίο αρχικοποιούνται οι βασικές μας μεταβλητές, όπως:
- ο χάρτης google
- οι markers για τα iot και την android συσκευή
- μεταβλητές κατάστασης για τις μεταβλητές (χάρτη και συσκευών, κυρίως για την διαχείριση λαθών στο command line)
- ένα instance παραλληλογράμμου

Επίσης βάση των πληροφοριών των δημιουργούμενων objects, μπορούμε βλέπουμε σε τι κατάσταση είναι μια συσκευή iot και ανάλογα αυτής προβαίνουμε στις ακόλουθες πράξεις:

- Αν είναι όλοι οι αισθητήρες τις είναι ανεργοί, κάνουμε το instance του κύκλου re-render σε κόκκινο χρώμα, αλλιώς αν μεταβούμε σε έστω και έναν ενεργό αισθητήρα το κάνουμε πράσινο.
- Αντίστοιχα, με βάση της λογικής που υποδείχθηκε, αν έστω και μια από τις συσκευές βρίσκονται σε κατάσταση μέτριας επικινδυνότητας, αλλάζουμε τα markers τους σε κίτρινα θαυμαστικά.
- Αν είναι και οι δύο σε μέτρια επικινδυνότητα τότε σχεδιάζουμε ένα κίτρινο παραλληλόγραμμο στη περιοχή που σχηματίζουν.
- Αν είναι σε υψηλή επικινδυνότητα έστω και μια από τις συσκευές (ή μεταβεί σε αυτή την κατάσταση) τότε κάνουμε το παραλληλόγραμμο κόκκινο.
- Εννοείται πως αν και οι δύο είναι σε κατάσταση υψηλής επικινδυνότητας, σχεδιάζουμε ένα κόκκινο παραλληλόγραμμο.
