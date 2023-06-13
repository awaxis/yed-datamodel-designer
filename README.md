# yed-datamodel-designer
DataModel designer for yEd Graph Editor

![DataModelDesigner](https://github.com/awaxis/yed-datamodel-designer/blob/main/DataModelDesigner.png?raw=true)

A conceptual data model diagram created with yEd helps to design and manage database entities and their relationships.

Each group represents a database table or entity, nodes inside group represent table fields, while edges represent the relationships between tables.

This diagram is based on custom shape to represent the data elements (table fields) with extra labels to identify the PK and FK for the constraints, that allow to link fields together for better representation of relationships.

Custom properties are used to add more detail about the entities and fields.

Export your design to SQL or CSV
--
The datamodel can be exported to CSV and/or SQL (PostgreSQL dialect) using JMeter script

Usage
---

jmeter -n -t /path/to/DataModelExporter.jmx
