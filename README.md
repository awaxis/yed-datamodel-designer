# yed-datamodel-designer
DataModel designer for yEd Graph Editor

![DataModelDesigner](https://github.com/awaxis/yed-datamodel-designer/blob/main/DataModelDesigner.png?raw=true)

A conceptual data model diagram created with yEd helps to design and manage database entities and their relationships.

Each group represents a database table or entity, nodes inside group represent table fields, while edges represent the relationships between tables.

This diagram is based on custom shape to represent the data elements (table fields) with extra labels to identify the PK and FK for the constraints, that allow to link fields together for better representation of relationships.

Custom properties are used to add more detail about the entities and fields.

Export your design to SQL or CSV
--
The datamodel can be exported to CSV and/or SQL (PostgreSQL dialect) using Groovy script

Usage
---
```
groovy DataModelExporter.groovy <GraphMLFile> <OutputFile>
  -d, --output-dir=<outputDir>
                             Output directory for generated files
      -ddl, --generate-ddl   Output SQL file with Entities definitions(default:
                               false)
      -ecf, --entities-csv-file=<entitiesCsvFile>
                             Output CSV file with Entities definitions
      -esf, --entities-sql-file=<entitiesSqlFile>
                             Output SQL file with Entities definitions
      -fcf, --fields-csv-file=<fieldsCsvFile>
                             Output CSV file with Entities fields definitions
      -fkai, --foreign-key-as-index
                             Generate Foreign Key as Index in DDL SQL file
                               (default: false)
      -fls, --field-list-separator=<fieldListSeparator>
                             Field list separator for the output CSV file
      -fs, --field-separator=<fieldSeparator>
                             Field separator for the output CSV file
  -g, --graphml-file=<graphMLFile>
                             Input GraphML file with DataModel
  -h, --help                 Show usage information
      -ld, --log-dir=<logDir>
                             Output directory for log files
      -lf, --log-file=<logFile>
                             Log file for the application
      -ls, --line-separator=<lineSeparator>
                             Line separator for the output CSV file
```
Examples:
```
# groovy DataModelExporter.groovy -g=DataModelDesigner.graphml -d=dist -ld=log -fs='\t' -fls=';' -ddl -fkai
# groovy DataModelExporter.groovy -g=DataModelDesigner.graphml -d=dist -ld=log -fs=',' -fls='|' -ecf=entities.csv -fcf=fields.csv
```