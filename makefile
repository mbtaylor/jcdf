
JAVAC = javac
JAVA = java
JAR = jar
JAVADOC = javadoc

JARFILE = cdf.jar
STILJAR = stil.jar

JSRC = \
       AttributeDescriptorRecord.java \
       AttributeEntryDescriptorRecord.java \
       Buf.java \
       CdfDescriptorRecord.java \
       CdfFormatException.java \
       CompressedCdfRecord.java \
       CompressedParametersRecord.java \
       CompressedVariableValuesRecord.java \
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
       DataType.java \
       GlobalAttribute.java \
       NumericEncoding.java \
       RecordMap.java \
       Shaper.java \
       Variable.java \
       VariableAttribute.java \
       VdrVariable.java \
       WrapperBuf.java \
       \
       CdfDump.java \
       CdfList.java \
       \
       CdfStarTable.java \
       CdfTableBuilder.java \
       CdfTableProfile.java \
       \
       CefFormatException.java \
       CefReader.java \
       CefTableBuilder.java \
       CefValueType.java \

jar: $(JARFILE)

docs: $(JSRC)
	rm -rf docs
	mkdir docs
	$(JAVADOC) -classpath $(STILJAR) -quiet -d docs $(JSRC)

build: jar docs

clean:
	rm -rf $(JARFILE) tmp docs

$(JARFILE): $(JSRC) $(STILJAR)
	rm -rf tmp
	mkdir -p tmp
	$(JAVAC) -Xlint:unchecked -classpath $(STILJAR) -d tmp $(JSRC) \
            && $(JAR) cf $(JARFILE) -C tmp .
	rm -rf tmp

$(STILJAR):
	curl -L http://www.starlink.ac.uk/stil/stil.jar >$@

