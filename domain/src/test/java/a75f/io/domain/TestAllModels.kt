package a75f.io.domain

import a75f.io.domain.migration.DiffManger
import a75f.io.domain.migration.ModelValidator
import a75f.io.domain.migration.ModelValidator.Companion.isValidaModel
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFEquipDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import org.junit.Test

/**
 * Created by Manjunath K on 22-06-2023.
 */

class TestAllModels {

    /*@Test
    fun validateModels(){
        val diffManger = DiffManger(null)
        val metaData =  diffManger.getModelFileVersionDetails(DiffManger.NEW_VERSION)
        ModelValidator.validateAllDomainModels(metaData)
    }*/
}