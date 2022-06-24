# Create a release

The release is not created via the GitHub action but locally via Maven and then the package is imported into a release on GibtHub.

```
mvn release:prepare
mvn release:perform
```

Create a release on GitHub and upload the created archives. 
