Small Kotlin app that creates the MapLibre StreetComplete map style.

* Edit the `Main.kt` script that defines the mapstyles
* Press "Play" if you're using IntelliJ, or compile and run from the commandline: `kotlinc *.kt -include-runtime -d Main.jar` followed by `java -jar Main.jar`. This updates the json styles in the `demo/` directory.
* To test the style simply open the `demo/index.html` file in a browser. (If this doesn't work you could serve it from a local webserver: `python -m http.server` and view it from http://localhost:8000 )
