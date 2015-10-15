package org.metadatacenter.ingestors.geo;

import org.metadatacenter.ingestors.geo.metadata.ContributorName;
import org.metadatacenter.ingestors.geo.metadata.GEOMetadata;
import org.metadatacenter.ingestors.geo.metadata.Platform;
import org.metadatacenter.ingestors.geo.metadata.Protocol;
import org.metadatacenter.ingestors.geo.metadata.Sample;
import org.metadatacenter.ingestors.geo.metadata.Series;
import org.metadatacenter.models.investigation.Characteristic;
import org.metadatacenter.models.investigation.CharacteristicValue;
import org.metadatacenter.models.investigation.Contact;
import org.metadatacenter.models.investigation.Input;
import org.metadatacenter.models.investigation.Investigation;
import org.metadatacenter.models.investigation.Output;
import org.metadatacenter.models.investigation.ParameterValue;
import org.metadatacenter.models.investigation.Process;
import org.metadatacenter.models.investigation.ProtocolParameter;
import org.metadatacenter.models.investigation.Publication;
import org.metadatacenter.models.investigation.Study;
import org.metadatacenter.models.investigation.StudyAssay;
import org.metadatacenter.models.investigation.StudyFactor;
import org.metadatacenter.models.investigation.StudyGroupPopulation;
import org.metadatacenter.models.investigation.StudyProtocol;
import org.metadatacenter.models.investigation.StudyTime;
import org.metadatacenter.repository.model.DateValueElement;
import org.metadatacenter.repository.model.StringValueElement;
import org.metadatacenter.repository.model.URIValueElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.metadatacenter.repository.model.RepositoryFactory.createOptionalStringValueElement;
import static org.metadatacenter.repository.model.RepositoryFactory.createStringValueElement;

public class GEOMetadata2InvestigationConverter
{
  private final GEOMetadata geoMetadata;

  private static final String INVESTIGATION_TEMPLATE_ID = "Investigation";

  public GEOMetadata2InvestigationConverter(GEOMetadata geoMetadata)
  {
    this.geoMetadata = geoMetadata;
  }

  // TODO GEO Series variables and repeat fields
  public Investigation convert()
  {
    Series geoSeries = this.geoMetadata.getSeries();

    String templateID = INVESTIGATION_TEMPLATE_ID;
    StringValueElement title = createStringValueElement(geoSeries.getTitle());
    StringValueElement description = createStringValueElement(geoSeries.getSummary());
    StringValueElement identifier = createStringValueElement(geoSeries.getTitle());
    Optional<DateValueElement> submissionDate = Optional.empty();
    Optional<DateValueElement> publicReleaseDate = Optional.empty();
    Optional<StudyProtocol> studyProtocol = convertGEOProtocol2StudyProtocol(this.geoMetadata.getProtocol());
    Study study = convertGEOSeries2Study(this.geoMetadata.getSeries(), this.geoMetadata.getSamples(),
      geoMetadata.getPlatform(), studyProtocol);

    return new Investigation(templateID, title, description, identifier, submissionDate, publicReleaseDate,
      Collections.singletonList(study));

  }

  private Study convertGEOSeries2Study(Series geoSeries, Map<String, Sample> geoSamples, Optional<Platform> geoPlatform,
    Optional<StudyProtocol> studyProtocol)
  {
    StringValueElement title = createStringValueElement(geoSeries.getTitle());
    StringValueElement description = createStringValueElement(geoSeries.getSummary());
    StringValueElement identifier = createStringValueElement(geoSeries.getTitle());
    Optional<DateValueElement> submissionDate = Optional.empty();
    Optional<DateValueElement> publicReleaseDate = Optional.empty();
    Optional<URIValueElement> studyDesignType = Optional.empty();
    Optional<StudyAssay> studyAssay = convertGEOPlatform2StudyAssay(geoPlatform);
    List<Process> processes = convertGEOSamples2Processes(geoSamples, studyAssay, studyProtocol);
    List<StudyFactor> studyFactors = new ArrayList<>(); // TODO Not clear where these are in GEO.
    Optional<StudyGroupPopulation> studyGroupPopulation = Optional.empty(); // Not recorded in GEO
    List<Publication> publications = convertPubmedIDs2Publications(geoSeries.getPubmedIDs());
    List<Contact> contacts = convertGEOContributors2Contacts(geoSeries.getContributors());
    List<StudyAssay> studyAssays = studyAssay.isPresent() ?
      Collections.singletonList(studyAssay.get()) :
      Collections.emptyList();

    return new Study(title, description, identifier, submissionDate, publicReleaseDate, studyDesignType, processes,
      studyProtocol, studyAssays, studyFactors, studyGroupPopulation, publications, contacts);
  }

  private List<Process> convertGEOSamples2Processes(Map<String, Sample> geoSamples, Optional<StudyAssay> studyAssay,
    Optional<StudyProtocol> studyProtocol)
  {
    List<Process> processes = new ArrayList<>();

    for (String sampleName : geoSamples.keySet()) {
      Process process = convertGEOSample2Process(geoSamples.get(sampleName), studyAssay, studyProtocol);
      processes.add(process);
    }
    return processes;
  }

  private Process convertGEOSample2Process(Sample geoSample, Optional<StudyAssay> studyAssay,
    Optional<StudyProtocol> studyProtocol)
  {
    StringValueElement type = createStringValueElement("GEOSampleProcess");
    List<ParameterValue> hasParameterValue = new ArrayList<>(); // Stored via the study protocol
    Optional<StudyAssay> sampleStudyAssay = extractStudyAssayFromGEOSample(geoSample);
    org.metadatacenter.models.investigation.Sample sample = extractSampleFromGEOSample(geoSample);
    List<Input> hasInput = new ArrayList<>();
    List<Output> hasOutput = new ArrayList<>(); // TODO files
    hasInput.add(sample);

    return new Process(type, studyAssay.isPresent() ? studyAssay : sampleStudyAssay, studyProtocol, hasParameterValue,
      hasInput, hasOutput);
  }

  //  private final String sampleName;
  //  private final String title;
  //  private final List<String> rawDataFiles;
  //  private final Optional<String> celFile;
  //  private final Optional<String> expFile;
  //  private final Optional<String> chpFile;
  //  private final String sourceName;
  //  private final List<String> organisms;
  //  private final Map<String, String> characteristics; // characteristic -> value
  //  private final Optional<String> biomaterialProvider;
  //  private final String molecule;
  //  private final String label;
  //  private final String description;
  //  private final String platform;

  private org.metadatacenter.models.investigation.Sample extractSampleFromGEOSample(
    org.metadatacenter.ingestors.geo.metadata.Sample geoSample)
  {
    StringValueElement name = createStringValueElement(geoSample.getTitle());
    StringValueElement type = createStringValueElement(geoSample.getPlatform());
    Optional<StringValueElement> description = createOptionalStringValueElement(geoSample.getPlatform());
    Optional<StringValueElement> source = createOptionalStringValueElement(geoSample.getBiomaterialProvider());
    List<Characteristic> characteristics = convertGEOCharacteristics2Characteristics(geoSample.getCharacteristics());
    Optional<StudyTime> hasStudyTime = Optional.empty(); // Not present

    return new org.metadatacenter.models.investigation.Sample(name, type, description, source, characteristics,
      hasStudyTime);
  }

  private List<Characteristic> convertGEOCharacteristics2Characteristics(Map<String, String> geoCharacteristics)
  {
    List<Characteristic> characteristics = new ArrayList<>();

    for (String geoCharacteristicName : geoCharacteristics.keySet()) {
      String geoCharacteristicValue = geoCharacteristics.get(geoCharacteristicName);
      CharacteristicValue characteristicValue = new CharacteristicValue(
        createStringValueElement(geoCharacteristicValue));
      Characteristic characteristic = new Characteristic(createStringValueElement(geoCharacteristicName),
        Optional.of(characteristicValue));

      characteristics.add(characteristic);
    }

    return characteristics;
  }

  private Optional<StudyAssay> extractStudyAssayFromGEOSample(Sample geoSample)
  {
    return Optional.of(new StudyAssay(createStringValueElement(geoSample.getPlatform())));
  }

  private List<Contact> convertGEOContributors2Contacts(List<ContributorName> geoContributors)
  {
    List<Contact> contacts = new ArrayList<>();

    for (ContributorName contributorName : geoContributors) {
      Contact contact = new Contact(createStringValueElement(contributorName.getFirstName()),
        createStringValueElement(contributorName.getMiddleInitial()),
        createStringValueElement(contributorName.getLastName()));

      contacts.add(contact);
    }
    return contacts;
  }

  private Optional<StudyAssay> convertGEOPlatform2StudyAssay(Optional<Platform> geoPlatform)
  {
    if (geoPlatform.isPresent())
      return Optional.of(new StudyAssay(createStringValueElement(geoPlatform.get().getTitle()),
        createOptionalStringValueElement(Optional.of(geoPlatform.get().getDistribution())),
        createOptionalStringValueElement(Optional.of(geoPlatform.get().getTechnology()))));
    else
      return Optional.empty();
  }

  private Optional<StudyProtocol> convertGEOProtocol2StudyProtocol(Protocol geoProtocol)
  {
    StringValueElement name = createStringValueElement(geoProtocol.getLabel());
    StringValueElement description = createStringValueElement(geoProtocol.getValueDefinition());
    Optional<StringValueElement> type = Optional.empty();
    Optional<URIValueElement> uri = Optional.empty();
    Optional<StringValueElement> version = Optional.empty();
    List<ProtocolParameter> protocolParameters = new ArrayList<>();

    if (geoProtocol.getGrowth().isPresent())
      protocolParameters.add(createProtocolParameter("growth", geoProtocol.getGrowth().get()));

    if (geoProtocol.getTreatment().isPresent())
      protocolParameters.add(createProtocolParameter("treatment", geoProtocol.getTreatment().get()));

    protocolParameters.add(createProtocolParameter("extract", geoProtocol.getExtract()));
    protocolParameters.add(createProtocolParameter("label", geoProtocol.getLabel()));
    protocolParameters.add(createProtocolParameter("hyb", geoProtocol.getHyb()));
    protocolParameters.add(createProtocolParameter("scan", geoProtocol.getScan()));
    protocolParameters.add(createProtocolParameter("dataProcessing", geoProtocol.getDataProcessing()));
    protocolParameters.add(createProtocolParameter("valueDefinition", geoProtocol.getValueDefinition()));

    return Optional.of(new StudyProtocol(name, description, type, uri, version, protocolParameters));
  }

  private ProtocolParameter createProtocolParameter(String parameterName, String value)
  {
    return createProtocolParameter(parameterName, Optional.empty(), value, Optional.empty(), Optional.empty());
  }

  private ProtocolParameter createProtocolParameter(String parameterName, Optional<String> parameterDescription,
    String value, Optional<String> type, Optional<String> unit)
  {
    ParameterValue parameterValue = new ParameterValue(createStringValueElement(value),
      createOptionalStringValueElement(type), createOptionalStringValueElement(unit));

    return new ProtocolParameter(createStringValueElement(parameterName),
      createOptionalStringValueElement(parameterDescription), Optional.of(parameterValue));
  }

  private List<Publication> convertPubmedIDs2Publications(List<String> pubmedIDs)
  {
    List<Publication> publications = new ArrayList<>();

    for (String pubmedID : pubmedIDs) {
      StringValueElement pubmedIDValueElement = createStringValueElement(pubmedID);
      Publication publication = new Publication(pubmedIDValueElement);
      publications.add(publication);
    }

    return publications;
  }
}
