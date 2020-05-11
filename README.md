# apolline-alpha
Alternative version of the Android app

# Todo list 
* Fix InfluxDB freezing the app & service when connection fails (critical)
* Cleanup strings & internationalization
* Pre-fill Sensor page with last stored values in the internal database
* Internal database management (cleanup, display, etc...)

# How to use
* Tap "Configure" to change the various settings related to InfluxDB.
* Tap "Connection", wait for the Blue Sensor to appear in the list, tap the sensor to start the BLE service.
* The BLE service then configures the sensor, and starts receiving data. No further operation is required to start retrieving data in INfluxDB, as the BLE service handles synchronization.
* Tap "Sensor" to view the received data in real-time.
* Tap "Disconnect" to close the BLE service and disconnect from the Blue Sensor.
* Tap "Sync to InfluxDB" to manually send the values stored in the internal database to InfluxDB. Note: only the values stored since the last synchronization will be sent. 

# About InfluxDB sync
There is currently some issues when the InfluxDB server is not available. Please ensure the server is running before trying to sync anything. Most of the times, a connection failure won't do anything nasty, but there are cases where the connection attempt does not timeout and freezes the whole BLE service, requiring a phone restart.

While the data received from the sensor are kept into an internal database, each value is removed from the database once it has been successfully synced to InfluxDB. Any failed attempt to synchronize a value will keep it into the internal database.

## Automatic synchronization
You can configure the app to automatically upload data to an InfluxDB server at a specific interval.

The server IP, user name, password and synchronization delay can be configured in the app's Settings. Tick the "InfluxDB sync" checkbox to enable automatic synchronization.
Currently, the table structure in InfluxDB is hard-coded, and is as following :
* Database : sensor_data
* Measurement : sensor_values
* Time : GPS time
* Fields :
** PM1
** PM2.5
** PM10
** Temperature
** Volt (battery power)
** Local time

## Manual synchronization
If the "InfluxDB sync" checkbox is unticked, the received data are kept into an internal database instead. You can tap the "Sync to InfluxDB" button in the main menu to manually send all the data to the InfluxDB server configured in the app's Settings.
