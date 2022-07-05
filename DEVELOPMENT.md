# Create a release

The release is not created via the GitHub action but locally via Maven and then the package is imported/uploaded into a release on GitHub.

```
mvn release:prepare
mvn release:perform
```

Create a release on GitHub and upload the created archives. 
