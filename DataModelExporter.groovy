def generateDDL = vars.get('DataModelDDL')
def fKeyAsIndex = vars.get('DataModelDDLFKeyAsIndex')
log.info('Loading "' + vars.get('DataModelGraphMLFile') + '" file');
def graphMLFile = new File(vars.get('DataModelGraphMLFile'))
def graphml = new XmlSlurper().parseText(graphMLFile.getText('UTF-8'))

log.info('Node parsing started')
def lineSeparator = System.getProperty('line.separator', '\n')
def fieldSeparator = System.getProperty('field.separator', ';')
def fieldDefinitions = [
    '#'
    , 'Designer Id'
    , 'Entity'
    , 'Entity Name'
    , 'Entity API Name'
    , 'Field Index'
    , 'Field Name'
    , 'Primary Key'
    , 'Foreign Key'
    , 'Indexed'
    , 'Unique'
    , 'Data Type'
    , 'Data Length'
    , 'Default Value'
    , 'Required'
    , 'Tracking'
    , 'Description'
].join(fieldSeparator)

def entityDefinitions = [
    'Designer Id'
    , 'Entity'
    , 'Name'
    , 'API Name'
    , 'Rank'
    , 'Scope'
    , 'Record Tracking'
    , 'Impl. Status'
    , 'Description'
].join(fieldSeparator)

def dataModelDDL = ''

def isEntityKey = graphml.key.findAll {it -> it.@'attr.name' == 'Is Entity' && it.@for == 'node'}
def isCustomTypeKey = graphml.key.findAll {it -> it.@'attr.name' == 'Is Custom Type' && it.@for == 'node'}
def isEnumTypeKey = graphml.key.findAll {it -> it.@'attr.name' == 'Is Enum Type' && it.@for == 'node'}
def entityNameKey = graphml.key.findAll {it -> it.@'attr.name' == 'Entity Name' && it.@for == 'node'}
def entityLabelKey = graphml.key.findAll {it -> it.@'attr.name' == 'Entity Label' && it.@for == 'node'}
def entityScopeKey = graphml.key.findAll {it -> it.@'attr.name' == 'Business Object' && it.@for == 'node'}
def fieldNameKey = graphml.key.findAll {it -> it.@'attr.name' == 'Field Name' && it.@for == 'node'}
def fieldPKKey = graphml.key.findAll {it -> it.@'attr.name' == 'Primary Key' && it.@for == 'node'}
def fieldSKKey = graphml.key.findAll {it -> it.@'attr.name' == 'Secondary Key' && it.@for == 'node'}
def fieldFKKey = graphml.key.findAll {it -> it.@'attr.name' == 'Foreign Key' && it.@for == 'node'}
def fieldDataTypeKey = graphml.key.findAll {it -> it.@'attr.name' == 'Field Type' && it.@for == 'node'}
def fieldLengthKey = graphml.key.findAll {it -> it.@'attr.name' == 'Field Length' && it.@for == 'node'}
def fieldDefaultValueKey = graphml.key.findAll {it -> it.@'attr.name' == 'Default Value' && it.@for == 'node'}
def fieldIndexedKey = graphml.key.findAll {it -> it.@'attr.name' == 'Indexed' && it.@for == 'node'}
def fieldUniqueKey = graphml.key.findAll {it -> it.@'attr.name' == 'Unique' && it.@for == 'node'}
def fieldRequiredKey = graphml.key.findAll {it -> it.@'attr.name' == 'Required' && it.@for == 'node'}
def fieldTrackingKey = graphml.key.findAll {it -> it.@'attr.name' == 'Enable Tracking' && it.@for == 'node'}
def fieldNullableKey = graphml.key.findAll {it -> it.@'attr.name' == 'Nullable' && it.@for == 'node'}
def fieldDefKey = graphml.key.findAll {it -> it.@'yfiles.type' == 'nodegraphics' && it.@for == 'node'}
def nodeDescriptionKey = graphml.key.findAll {it -> it.@'attr.name' == 'description' && it.@for == 'node'}
def entityInheritFromKey = graphml.key.findAll {it -> it.@'attr.name' == 'Inherit From' && it.@for == 'node'}
def entityPartitionByKey = graphml.key.findAll {it -> it.@'attr.name' == 'Partition By' && it.@for == 'node'}
def entityPartitionTypeKey = graphml.key.findAll {it -> it.@'attr.name' == 'Partition Type' && it.@for == 'node'}
def entityGroupKey = graphml.key.findAll {it -> it.@'attr.name' == 'Entity Group' && it.@for == 'node'}

def sortedNodes = graphml.graph.node.list().sort{it.data.findAll {d -> d.@key == entityGroupKey.@id}}
sortedNodes.eachWithIndex { node, nIndex -> {
    if(node.@'yfiles.foldertype' == 'group') {
        def isEntity = node.data.findAll {it -> it.@key == isEntityKey.@id}
        def isCustomType = node.data.findAll {it -> it.@key == isCustomTypeKey.@id}
        def isEnumType = node.data.findAll {it -> it.@key == isEnumTypeKey.@id}
        
        if(isEntity == 'true' || isCustomType == 'true' || isEnumType == 'true') {
            def entityName = node.data.findAll {it -> it.@key == entityNameKey.@id}
            def unquotedEntityName = entityName.text().replaceAll('"', '')
            def entityLabel = node.data.findAll {it -> it.@key == entityLabelKey.@id}
            def entityApiName = unquotedEntityName.split('_').collect{ it.capitalize() }.join('')
            def recordTracking = node.data.findAll {it -> it.@key == fieldTrackingKey.@id}
            def entityBusinessScope = node.data.findAll {it -> it.@key == entityScopeKey.@id}
            def entityDescription = node.data.findAll {it -> it.@key == nodeDescriptionKey.@id}
            def entityInheritFrom = node.data.findAll {it -> it.@key == entityInheritFromKey.@id}
            def entityPartitionBy = node.data.findAll {it -> it.@key == entityPartitionByKey.@id}
            def entityPartitionType = node.data.findAll {it -> it.@key == entityPartitionTypeKey.@id}
            def entityGroup = node.data.findAll {it -> it.@key == entityGroupKey.@id}
            def entityRecord = [
                node.@id
                , entityLabel
                , unquotedEntityName
                , entityApiName
                , entityGroup
                , entityBusinessScope == 'true' ? 'Business' : 'Application'
                , recordTracking == 'true' ? 'Yes' : 'No'
                , 'No'
                , '"' + entityDescription + '"'
            ].join(fieldSeparator)
            entityDefinitions = entityDefinitions + lineSeparator + entityRecord

            if(generateDDL == 'yes') {
                if(isCustomType == 'true') {
                    dataModelDDL = dataModelDDL + lineSeparator + '-- ' + unquotedEntityName + ' type definition' + lineSeparator
                    dataModelDDL = dataModelDDL + 'CREATE TYPE ' + entityName + ' AS ('
                } else if(isEnumType == 'true') {
                    dataModelDDL = dataModelDDL + lineSeparator + '-- ' + entityName + ' enum definition' + lineSeparator
                    dataModelDDL = dataModelDDL + 'CREATE TYPE ' + entityName + ' AS ENUM ('
                } else {
                    dataModelDDL = dataModelDDL + lineSeparator + '-- ' + entityName + ' entity definition' + lineSeparator
                    dataModelDDL = dataModelDDL + 'CREATE TABLE IF NOT EXISTS ' + entityName + ' ('
                }
            }
            def ddlFieldSep = ','
            def ddlFieldPKDef = []
            def ddlEntityIndexes = []
            def sortedFields = node.graph.node.list().sort{
                it.data.findAll {d -> d.@key == fieldDefKey.@id}[0].GenericNode.Geometry.@y.toFloat()
            }
            sortedFields.eachWithIndex { fieldNode, fIndex -> {
                def fieldDef = fieldNode.data.findAll {it -> it.@key == fieldDefKey.@id}
                if(fieldDef.size() > 0) {
                    def fieldName = fieldNode.data.findAll {it -> it.@key == fieldNameKey.@id}
                    def fieldPK = fieldNode.data.findAll {it -> it.@key == fieldPKKey.@id}
                    def fieldSK = fieldNode.data.findAll {it -> it.@key == fieldSKKey.@id}
                    def fieldFK = fieldNode.data.findAll {it -> it.@key == fieldFKKey.@id}
                    def fieldDataType = fieldNode.data.findAll {it -> it.@key == fieldDataTypeKey.@id}
                    def fieldIndexed = fieldNode.data.findAll {it -> it.@key == fieldIndexedKey.@id}
                    def fieldUnique = fieldNode.data.findAll {it -> it.@key == fieldUniqueKey.@id}
                    def fieldRequired = fieldNode.data.findAll {it -> it.@key == fieldRequiredKey.@id}
                    def fieldLength = fieldNode.data.findAll {it -> it.@key == fieldLengthKey.@id}
                    def fieldTracking = fieldNode.data.findAll {it -> it.@key == fieldTrackingKey.@id}
                    def fieldDefaultValue = fieldNode.data.findAll {it -> it.@key == fieldDefaultValueKey.@id}
                    def fieldDescription = fieldNode.data.findAll {it -> it.@key == nodeDescriptionKey.@id}
                    def fieldNullable = fieldNode.data.findAll {it -> it.@key == fieldNullableKey.@id}
                    
                    def unquotedFieldName
                    if ( fieldName == null || fieldName == '') {
                        fieldName = fieldDef[0].GenericNode.NodeLabel[0]
                        unquotedFieldName = fieldName
                    } else {
                        unquotedFieldName = fieldName.text().replaceAll('"', '')
                    }
                    if(fieldDataType == '') {
                        if(fieldLength != '' && fieldLength != '0') {
                            fieldDataType = 'varchar'
                        } else {
                            fieldDataType = fieldDataTypeKey[0].default
                        }
                    }
    	       	    def fieldRecord = [
                        entityGroup
                        , node.@id
                        , entityLabel
                        , entityApiName
                        , unquotedEntityName
                        , fIndex 
                        , unquotedFieldName
                        , fieldPK
                        , fieldFK
                        , fieldIndexed
                        , fieldUnique
                        , fieldDataType
                        , fieldLength
                        , fieldDefaultValue 
                        , fieldRequired 
                        , fieldTracking 
                        , '"' + fieldDescription + '"'
                    ].join(fieldSeparator)
               	    fieldDefinitions = fieldDefinitions + lineSeparator + fieldRecord

                    log.info('Field '+unquotedEntityName+'_'+unquotedFieldName+' -> order : ' + fieldDef[0].GenericNode.Geometry.@y)

                    if(generateDDL == 'yes') {
                        def ddlFieldDef =  lineSeparator + '    '
                        if(isEnumType == 'true') {
                            ddlFieldDef = ddlFieldDef + '\'' + unquotedFieldName + '\''
                        } else {
                            ddlFieldDef = ddlFieldDef + fieldName + ' ' + fieldDataType
                            if(fieldLength != '' && fieldLength != '0') {
                                ddlFieldDef = ddlFieldDef + '(' + fieldLength + ')'
                            }

                            if(fieldNullable.size() == 0 || fieldNullable == 'true') {
                                ddlFieldDef = ddlFieldDef + ' NULL'
                            } else {
                                ddlFieldDef = ddlFieldDef + ' NOT NULL'
                            }
                            if(fieldDefaultValue != '') {
                                ddlFieldDef = ddlFieldDef + ' DEFAULT ' + fieldDefaultValue
                            }
                        }

                        dataModelDDL = dataModelDDL + ddlFieldDef + ddlFieldSep

                        if(fieldPK == 'true' || fieldSK == 'true' ) {
                            ddlFieldPKDef.add(fieldName)
                        }

                        if(fieldIndexed == 'true' || (fieldFK == 'true' && fKeyAsIndex == 'yes')) {
                            def entityIndex = 'CREATE '
                            if(fieldUnique == 'true') {
                                entityIndex = entityIndex + 'UNIQUE '
                            }
                            entityIndex = entityIndex + 'INDEX ' + unquotedEntityName + '_' + unquotedFieldName + '_idx ON ' + entityName + ' (' + fieldName + ');'
                            ddlEntityIndexes.add(entityIndex)
                        }
                    }
                }
            }}

            if(generateDDL == 'yes') {
                if(ddlFieldPKDef.size() > 0) {
                    dataModelDDL = dataModelDDL + lineSeparator + '    ' + 'CONSTRAINT ' + unquotedEntityName + '_pkey PRIMARY KEY (' + ddlFieldPKDef.join(',') + ')'
                } else {
                    // remove last field separator (last commas)
                    dataModelDDL = dataModelDDL.substring(0, dataModelDDL.length() - 1)
                }
                dataModelDDL = dataModelDDL + lineSeparator + ')'

                if(entityInheritFrom != '') {
                    dataModelDDL = dataModelDDL + lineSeparator + 'INHERITS (' + entityInheritFrom + ')'
                }
                if(entityPartitionBy != '') {
                    dataModelDDL = dataModelDDL + lineSeparator + 'PARTITION BY ' + entityPartitionType + ' (' + entityPartitionBy + ')'
                }
                dataModelDDL = dataModelDDL + ';' + lineSeparator
                if(ddlEntityIndexes.size() > 0) {
                    dataModelDDL = dataModelDDL + ddlEntityIndexes.join(lineSeparator) + lineSeparator
                }
            }
        }
    }
}}
log.info('Entity definitions : \n' + entityDefinitions)
log.info('Field definitions : \n' + fieldDefinitions)
log.info('DDL definitions : \n' + dataModelDDL)

log.info('Writing DataModel entities to "' + vars.get('DataModelEntitiesCsvFile') + '" file');
def csvFile = new File(vars.get('DataModelEntitiesCsvFile'))
csvFile.write(entityDefinitions)

log.info('Writing DataModel fields to "' + vars.get('DataModelFieldsCsvFile') + '" file');
csvFile = new File(vars.get('DataModelFieldsCsvFile'))
csvFile.write(fieldDefinitions)

log.info('Writing DataModel DDL to "' + vars.get('DataModelSqlFile') + '" file');
csvFile = new File(vars.get('DataModelSqlFile'))
csvFile.write(dataModelDDL)
