import java.util.Properties
import java.io.FileInputStream

val credsFile = file("../secrets/creds.txt")
val credsProps = Properties()
if (credsFile.exists()) {
    credsProps.load(FileInputStream(credsFile))
}

println("Loaded Cloudinary Key: " + credsProps.getProperty("CLOUDINARY_API_KEY"))
