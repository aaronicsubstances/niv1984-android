# NIV1984

## Running Preprocessor

```bash
mvn -f nivbible-preprocessor\pom.xml clean package
cd scraped
mvn -f ..\nivbible-preprocessor\pom.xml exec:java -Dexec.mainClass=com.aaronicsubstances.nivbible.preprocessor.Main
```