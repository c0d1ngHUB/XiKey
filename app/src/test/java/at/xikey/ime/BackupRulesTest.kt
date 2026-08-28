package at.xikey.ime

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesTest {
    @Test fun `backup rules exclude local learning preferences`() {
        val rulesDir = File("src/main/res/xml")
        val dataExtractionRules = File(rulesDir, "data_extraction_rules.xml").readText()
        val backupRules = File(rulesDir, "backup_rules.xml").readText()

        assertTrue(dataExtractionRules.contains("<exclude domain=\"sharedpref\" path=\"xikey_preferences.xml\" />"))
        assertTrue(dataExtractionRules.contains("<cloud-backup>"))
        assertTrue(dataExtractionRules.contains("<device-transfer>"))
        assertTrue(backupRules.contains("<exclude domain=\"sharedpref\" path=\"xikey_preferences.xml\" />"))
    }
}
