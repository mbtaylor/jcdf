
JAVAC = javac
JAVA = java
JAR = jar
JAVADOC = javadoc

JARFILE = cdf.jar
STILJAR = stil.jar

TEST_CDFS = data/*.cdf

JSRC = \
       BankBuf.java \
       Buf.java \
       Bufs.java \
       Pointer.java \
       SimpleNioBuf.java \
       WrapperBuf.java \
       \
       AttributeDescriptorRecord.java \
       AttributeEntryDescriptorRecord.java \
       CdfDescriptorRecord.java \
       CompressedCdfRecord.java \
       CompressedParametersRecord.java \
       CompressedVariableValuesRecord.java \
       GlobalDescriptorRecord.java \
       Record.java \
       RecordFactory.java \
       RecordPlan.java \
       SparsenessParametersRecord.java \
       UnusedInternalRecord.java \
       VariableDescriptorRecord.java \
       VariableIndexRecord.java \
       VariableValuesRecord.java \
       CdfField.java \
       \
       BitExpandInputStream.java \
       Compression.java \
       NumericEncoding.java \
       RunLengthInputStream.java \
       RecordMap.java \
       DataReader.java \
       EpochFormatter.java \
       \
       CdfContent.java \
       GlobalAttribute.java \
       VariableAttribute.java \
       Variable.java \
       CdfInfo.java \
       CdfReader.java \
       DataType.java \
       Shaper.java \
       CdfFormatException.java \
       \
       CdfDump.java \
       CdfList.java \
       LogUtil.java \
       \
       CdfStarTable.java \
       CdfTableBuilder.java \
       CdfTableProfile.java \
       \
       ExampleTest.java \
       SameTest.java \
       \
       CefFormatException.java \
       CefReader.java \
       CefTableBuilder.java \
       CefValueType.java \

jar: $(JARFILE)

docs: $(JSRC) package-info.java
	rm -rf docs
	mkdir docs
	$(JAVADOC) -classpath $(STILJAR) -quiet -d docs \
                   $(JSRC) package-info.java

build: jar docs

test: extest convtest

convtest: $(JARFILE) $(TEST_CDFS)
	rm -rf tmp; \
	mkdir tmp; \
	for f in $(TEST_CDFS); \
        do \
           files=`./cdfvar.sh -outdir tmp -report $$f`; \
           cmd="java -ea -classpath $(JARFILE) cdf.test.SameTest $$files"; \
           ./cdfvar.sh -outdir tmp -create $$f && \
           echo $$cmd && \
           $$cmd || \
           break; \
        done && \
        rm -rf tmp

extest: $(JARFILE)
	java -ea -classpath $(JARFILE) cdf.test.ExampleTest \
             data/example1.cdf data/example2.cdf

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

