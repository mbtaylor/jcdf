
JAVAC = javac
JAVA = java
JAR = jar
JAVADOC = javadoc

JARFILE = cdf.jar

JSRC = \
       AttributeDescriptorRecord.java \
       AttributeEntryDescriptorRecord.java \
       Buf.java \
       CdfDescriptorRecord.java \
       CdfFormatException.java \
       CompressedCdfRecord.java \
       CompressedParametersRecord.java \
       CompressedVariableValuesRecord.java \
       DataType.java \
       GlobalDescriptorRecord.java \
       NioBuf.java \
       Pointer.java \
       Record.java \
       RecordFactory.java \
       RecordPlan.java \
       SparsenessParametersRecord.java \
       UnusedInternalRecord.java \
       VariableDescriptorRecord.java \
       VariableIndexRecord.java \
       VariableValuesRecord.java \
       \
       CdfContent.java \
       CdfInfo.java \
       CdfReader.java \
       Compression.java \
       DataReader.java \
       DataReaderFactory.java \
       Entry.java \
       GlobalAttribute.java \
       RecordMap.java \
       Variable.java \
       VariableAttribute.java \
       VdrVariable.java \
       WrapperBuf.java \

jar: $(JARFILE)

docs: $(JSRC)
	rm -rf docs
	mkdir docs
	$(JAVADOC) -quiet -d docs $(JSRC)

build: jar docs

clean:
	rm -rf $(JARFILE) tmp docs

$(JARFILE): $(JSRC)
	rm -rf tmp
	mkdir -p tmp
	$(JAVAC) -Xlint:unchecked -d tmp $(JSRC) \
            && $(JAR) cf $(JARFILE) -C tmp .
	rm -rf tmp

