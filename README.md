# apolline-alpha
Alternative version of the Android app

# Todo list 
* Fix InfluxDB freezing the app & service when connection fails (critical)
* Synchronize with InfluxDB at a specific interval, instead of synchronizing when data is received
* Keep a database of received values in order to synchronize data when there is a connection loss
* Display PMx values under the graph
* Sync local time and GPS time in InfluxDB
