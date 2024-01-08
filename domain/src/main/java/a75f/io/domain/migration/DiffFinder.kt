package a75f.io.domain.migration

import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import io.seventyfivef.domainmodeler.client.DiffType
import io.seventyfivef.domainmodeler.client.ModelDiff
import io.seventyfivef.domainmodeler.client.ModelDirective

/**
 * Created by Manjunath K on 16-06-2023.
 */

class DiffFinder {
    /**
     * Function to calculate the difference of two model definitions
     * @param left original model for reference
     * @param right new Model to find the diff
     * @return ModelDiff return diff details
     */
    fun calculateDiff(left: ModelDirective, right: ModelDirective): ModelDiff {
        return ModelDiff(left, right, groupByTagNames = false)
    }

    /**
     * @param domainName it main model equipName
     * @param entityConfiguration diff detilas to add delete and update
     * @param diffType type of change
     */
    private fun updateEntityConfiguration(domainName: String?, entityConfiguration: EntityConfiguration, diffType: DiffType){
        domainName.let {
            when (diffType) {
                DiffType.ADDED ->entityConfiguration.tobeAdded.add(EntityConfig(it!!))
                DiffType.UPDATED ->entityConfiguration.tobeUpdated.add(EntityConfig(it!!))
                DiffType.REMOVED ->entityConfiguration.tobeDeleted.add(EntityConfig(it!!))
                else -> { }
            }
        }

    }

    /**
     * Function to find the change point diff details
     * @param diff diff of model definitions
     * @param entityConfiguration entity configuration to update the result
     */
    fun findPointUpdate(diff: ModelDiff, entityConfiguration: EntityConfiguration) {

        diff.let { it ->
            it.points?.diff?.forEach {
                if(it.diffType == DiffType.REMOVED) {
                    updateEntityConfiguration(it.left?.point?.domainName!!,entityConfiguration,it.diffType)
                }
                else if(it.diffType != DiffType.EQUAL) {
                    updateEntityConfiguration(it.right?.point?.domainName!!,entityConfiguration,it.diffType)
                }
            }
        }


    }

    /**
     * Function to check the diff in model name and tags
     * @param equipDomainName domain name of the equip
     * @param diff diff of models definitions
     * @param entityConfiguration entity configuration
     */
    fun findEquipUpdate(equipDomainName: String ,diff: ModelDiff, entityConfiguration: EntityConfiguration) {
        diff.let {
            if (diff.name?.diffType != DiffType.EQUAL
                || diff.tags!!.count.left != diff.tags!!.count.right) {
                entityConfiguration.tobeAdded.add(EntityConfig(equipDomainName))
                updateEntityConfiguration(equipDomainName, entityConfiguration, diff.name!!.diffType)
                return
            }

            diff.tags?.right?.forEach {
                if (it.diffType == DiffType.ADDED || it.diffType == DiffType.UPDATED
                    || it.diffType == DiffType.REMOVED) {
                    updateEntityConfiguration(equipDomainName, entityConfiguration, it.diffType)
                }
            }

            diff.tags?.left?.forEach {
                if (it.diffType == DiffType.REMOVED) {
                    updateEntityConfiguration(equipDomainName, entityConfiguration, it.diffType)
                }
            }
        }


    }

 }