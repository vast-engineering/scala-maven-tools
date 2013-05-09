//ensure that the jarfile was written
assert new File( basedir, "target/simple-it-1.0-SNAPSHOT.jar" ).isFile()

//ensure that a surefire report was written
assert new File( basedir, "target/surefire-reports/TEST-com.test.HelloWorldTest.xml" ).isFile()



