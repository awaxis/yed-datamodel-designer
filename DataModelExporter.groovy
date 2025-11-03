@GrabConfig(systemClassLoader=true)
@Grab('org.codehaus.groovy:groovy-cli-picocli:3.0.1')
@Grab('info.picocli:picocli:4.2.0')
import groovy.cli.picocli.CliBuilder;
import groovy.xml.*;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

def cli = new CliBuilder(usage:'DataModelExporter <GraphMLFile> <OutputFile>');
cli.with {
    h   longOpt: 'help', 'Show usage information'
    g   longOpt: 'graphml-file', args: 1, argName: 'graphMLFile', type: String, required: true, 'Input GraphML file with DataModel'
    d   longOpt: 'output-dir', args: 1, argName: 'outputDir', type: String, required: false, 'Output directory for generated files'
    ld  longOpt: 'log-dir', args: 1, argName: 'logDir', type: String, required: false, 'Output directory for log files'
    lf  longOpt: 'log-file', args: 1, argName: 'logFile', type: String, required: false, 'Log file for the application'
    ecf longOpt: 'entities-csv-file', args: 1, argName: 'entitiesCsvFile', type: String, required: false, 'Output CSV file with Entities definitions'
    fcf longOpt: 'fields-csv-file', args: 1, argName: 'fieldsCsvFile', type: String, required: false, 'Output CSV file with Entities fields definitions'
    esf longOpt: 'entities-sql-file', args: 1, argName: 'entitiesSqlFile', type: String, required: false, 'Output SQL file with Entities definitions'
    ddl longOpt: 'generate-ddl', args: 0, argName: 'generateDDL', type: Boolean, required: false, 'Output SQL file with Entities definitions(default: false)'
    fkai longOpt: 'foreign-key-as-index', args: 0, argName: 'foreignKeyAsIndex', type: Boolean, required: false, 'Generate Foreign Key as Index in DDL SQL file (default: false)'
    fs longOpt: 'field-separator', args: 1, argName: 'fieldSeparator', type: String, required: false, 'Field separator for the output CSV file'
    ls longOpt: 'line-separator', args: 1, argName: 'lineSeparator', type: String, required: false, 'Line separator for the output CSV file'
    fls longOpt: 'field-list-separator', args: 1, argName: 'fieldListSeparator', type: String, required: false, 'Field list separator for the output CSV file'
}

def args = cli.parse(args);
assert args != null : 'Invalid arguments. Use -h for help.'
assert args.g || args.graphMLFile : 'GraphML file is required. Use -g or --graphml-file.'

def dataModelGraphMLFileWithoutExt = args.g.take(args.g.lastIndexOf('.'))

System.setProperty("java.util.logging.SimpleFormatter.format", '%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS [%4$-6s] %5$s%6$s%n');
Logger log = Logger.getLogger("DataModelExporter")
def logFileName = args.lf
if(isNullOrEmpty(logFileName)) {
    logFileName = dataModelGraphMLFileWithoutExt + '.log'
}
if(!isNullOrEmpty(args.ld)) {
    logFileName = args.ld + '/' + logFileName
}
FileHandler fh = new FileHandler(logFileName)
log.addHandler(fh)
SimpleFormatter formatter = new SimpleFormatter()
fh.setFormatter(formatter)

log.info('Loading "' + args.g + '" file');
def graphMLFile = new File(args.g)
def graphml = new XmlSlurper().parseText(graphMLFile.getText('UTF-8'))

// load the document metadata
def documentVersionKey = graphml.key.findAll {it -> it.@'attr.name' == 'Version' && it.@for == 'graph'}
def documentRevisionKey = graphml.key.findAll {it -> it.@'attr.name' == 'Revision' && it.@for == 'graph'}
def documentTitleKey = graphml.key.findAll {it -> it.@'attr.name' == 'Title' && it.@for == 'graph'}
def documentVersion = graphml.graph.data.findAll {it -> it.@key == documentVersionKey.@id}
def documentTitle = graphml.graph.data.findAll {it -> it.@key == documentTitleKey.@id}
def documentRevision = graphml.graph.data.findAll {it -> it.@key == documentRevisionKey.@id}

if(documentTitle != null && documentTitle != '') {
    dataModelGraphMLFileWithoutExt = documentTitle.text().replaceAll(' ', '-')
    if(documentVersion != null && documentVersion != '') {
        dataModelGraphMLFileWithoutExt = dataModelGraphMLFileWithoutExt + '-v' + documentVersion.text()
    }
    if(documentRevision != null && documentRevision != '') {
        dataModelGraphMLFileWithoutExt = dataModelGraphMLFileWithoutExt + 'r' + documentRevision.text()
    }
}

def dataModelEntitiesCsvFile = args.ecf
if(isNullOrEmpty(dataModelEntitiesCsvFile)) {
    dataModelEntitiesCsvFile = dataModelGraphMLFileWithoutExt + '-Entities.csv'
}
def dataModelFieldsCsvFile = args.fcf
if(isNullOrEmpty(dataModelFieldsCsvFile)) {
    dataModelFieldsCsvFile = dataModelGraphMLFileWithoutExt + '-Fields.csv'
}
def dataModelSqlFile = args.esf
if(isNullOrEmpty(dataModelSqlFile)) {
    dataModelSqlFile = dataModelGraphMLFileWithoutExt + '.sql'
}

if(!isNullOrEmpty(args.d)) {
    dataModelEntitiesCsvFile = args.d + '/' + dataModelEntitiesCsvFile
    dataModelFieldsCsvFile = args.d + '/' + dataModelFieldsCsvFile
    dataModelSqlFile = args.d + '/' + dataModelSqlFile
}

def generateDDL = (args.ddl instanceof Boolean ? args.ddl : false)
def fKeyAsIndex = (args.fkai instanceof Boolean ? args.fkai : false)

log.info('DataModel Entities file: ' + dataModelEntitiesCsvFile);
log.info('DataModel fields file: ' + dataModelFieldsCsvFile);
log.info('Generate DDL SQL file: ' + generateDDL);
if(generateDDL) {
    log.info('DataModel DDL file: ' + dataModelSqlFile);
}

static boolean isNullOrEmpty(String str) { return (str == null || str.allWhitespace) } 

log.info('Node parsing started')
def lineSeparator = args.lineSeparator != null && args.lineSeparator != '' && args.lineSeparator != false ? args.lineSeparator.replace('\\n', '\n').replace('\\r', '\r') : System.getProperty('line.separator', '\n')
def fieldSeparator = args.fieldSeparator != null && args.fieldSeparator != '' && args.fieldSeparator != false ? args.fieldSeparator.replace('\\t', '\t') : System.getProperty('field.separator', ';')
def fieldListSeparator = args.fieldListSeparator != null && args.fieldListSeparator != '' && args.fieldListSeparator != false ? args.fieldListSeparator.replace('\\t', '\t') : ','
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

            if(generateDDL) {
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

                    log.info('Field ' + unquotedEntityName + '_' + unquotedFieldName + ' -> order : ' + fieldDef[0].GenericNode.Geometry.@y)

                    if(generateDDL) {
                        def ddlFieldDef =  lineSeparator + '    '
                        if(isEnumType == 'true') {
                            ddlFieldDef = ddlFieldDef + '\'' + unquotedFieldName + '\''
                        } else {
                            ddlFieldDef = ddlFieldDef + fieldName + ' ' + fieldDataType
                            if(fieldLength != '' && fieldLength != '0') {
                                ddlFieldDef = ddlFieldDef + '(' + fieldLength + ')'
                            }
                            if(isCustomType != 'true') {
                                if(fieldNullable.size() == 0 || fieldNullable == 'true') {
                                    ddlFieldDef = ddlFieldDef + ' NULL'
                                } else {
                                    ddlFieldDef = ddlFieldDef + ' NOT NULL'
                                }
                            }
                            if(fieldDefaultValue != '') {
                                ddlFieldDef = ddlFieldDef + ' DEFAULT ' + fieldDefaultValue
                            }
                        }

                        dataModelDDL = dataModelDDL + ddlFieldDef + ddlFieldSep

                        if(fieldPK == 'true' || fieldSK == 'true' ) {
                            ddlFieldPKDef.add(fieldName)
                        }

                        if(fieldIndexed == 'true' || (fieldFK == 'true' && fKeyAsIndex)) {
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

            if(generateDDL) {
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

log.info('Writing DataModel entities to "' + dataModelEntitiesCsvFile + '" file');
log.info('Entity definitions : \n' + entityDefinitions)
def outputFile = new File(dataModelEntitiesCsvFile)
outputFile.write(entityDefinitions)

log.info('Writing DataModel fields to "' + dataModelFieldsCsvFile + '" file');
log.info('Field definitions : \n' + fieldDefinitions)
outputFile = new File(dataModelFieldsCsvFile)
outputFile.write(fieldDefinitions)

if(generateDDL) {
    log.info('DDL definitions : \n' + dataModelDDL)
    log.info('Writing DataModel DDL to "' + dataModelSqlFile + '" file');
    outputFile = new File(dataModelSqlFile)
    outputFile.write(dataModelDDL)
}
