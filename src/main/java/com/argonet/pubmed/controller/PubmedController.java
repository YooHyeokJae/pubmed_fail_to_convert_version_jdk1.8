package com.argonet.pubmed.controller;

import com.argonet.pubmed.service.PubmedService;
import com.argonet.pubmed.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Slf4j
@Controller
public class PubmedController {

    @Autowired
    PubmedService pubmedService;

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/test")
    public String test(){
        return "test";
    }

    private void downloadUpdatedGZFile() throws IOException {
        String url = "https://ftp.ncbi.nlm.nih.gov/pubmed/updatefiles/";
        String html = fn_crawling(url);
        html = html.split("<a href=\"README.txt\">")[1];
        String[] splitArr = html.split("<a href=\"");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(new Date());

        for (String line : splitArr) {
            if (!line.contains("pubmed24n")) continue;
            String fileUrl = line.split("\">")[0];
            if(!fileUrl.endsWith(".gz")) continue;
            String updDt = line.split("</a>")[1];
            updDt = updDt.trim().substring(0, 10);

            if(updDt.equals(date)){
                String gzFile = downFile(fileUrl);
                String dest = "C:/pubmed/gzFiles/unzip/";
                String xmlFile = decompressGzFile(dest, gzFile);

                log.info(xmlFile);
                openXmlFile(xmlFile);

                if(xmlFile != null){
                    File f = new File(xmlFile);
                    boolean delete = f.delete();
                }
            }
        }
    }

    private String fn_crawling(String url) throws IOException {
        String html;

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();

                BufferedReader br;

                StringBuilder sb = new StringBuilder();
                String line;
                try {
                    br = new BufferedReader(new InputStreamReader(entity.getContent()));
                    while ((line = Objects.requireNonNull(br).readLine()) != null) {
                        sb.append(line).append("\r\n");
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }

                html = sb.toString();

                EntityUtils.consume(entity);
            }
        }
        return html;
    }

    private String downFile(String pUrl) throws IOException {

        File f = new File("C:/pubmed/gzFiles/" + pUrl);
        if(f.exists()) {
            log.info("skip");
            return "C:/pubmed/gzFiles/"+pUrl;
        }

        URL url = new URL("https://ftp.ncbi.nlm.nih.gov/pubmed/updatefiles/" + pUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("user-Agent", "Mozilla/5.0");

        int resCode = conn.getResponseCode();
        if(resCode == HttpURLConnection.HTTP_OK){
            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(new File("C:/pubmed/gzFiles", pUrl));

            final int BUFFER_SIZE = 4096;
            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];

            while((bytesRead = is.read(buffer)) != -1){
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            is.close();
        }
        return "C:/pubmed/gzFiles/"+pUrl;
    }

    private String decompressGzFile(String dest, String file) {
        if(!file.contains(".gz"))   return null;
        String xmlFile = dest + file.substring(file.lastIndexOf("/")+1, file.lastIndexOf(".gz"));

        try {
            FileInputStream fis = new FileInputStream(file);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(xmlFile);
            byte[] buffer = new byte[1024];
            int len;

            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }

            fos.close();
            gis.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return xmlFile;
    }

    private void openXmlFile(String xmlFile){
        log.info("{} 여는중", xmlFile);
        File file = new File(xmlFile);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // List variable for union insert
            List<AuthorVO> authorVOList = new ArrayList<>();
            List<AffiliationVO> affiliationVOList = new ArrayList<>();
            List<MeshHeadingVO> meshHeadingVOList = new ArrayList<>();
            List<ChemicalVO> chemicalVOList = new ArrayList<>();
            List<GrantVO> grantVOList = new ArrayList<>();
            List<ReferenceVO> referenceVOList = new ArrayList<>();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlFile);

            NodeList pubmedArticles = doc.getElementsByTagName("PubmedArticle");
            for (int i=0; i<pubmedArticles.getLength(); i++) {  // ~30000
                Node node = pubmedArticles.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element E_pubmedArticle = (Element) node;

                    Element E_article = (Element) E_pubmedArticle.getElementsByTagName("Article").item(0);

                    // PUBMED_ARTICLE INSERT
                    parsingArticleInfo(E_pubmedArticle);

                    // PUBMED_ARTICLE_AUTHOR, PUBMED_ARTICLE_AFFILIATION, PUBMED_ARTICLE_MESH INSERT
                    parsingAuthorAndMesh(E_pubmedArticle, authorVOList, affiliationVOList, meshHeadingVOList);

                    // PUBMED_ARTICLE_CHEMICAL, PUBMED_ARTICLE_GRANT, PUBMED_ARTICLE_REFERENCE INSERT
                    parsingChemicalGrantReference(E_pubmedArticle, chemicalVOList, grantVOList, referenceVOList);

                    // union insert
                    if(i%100 == 99){
                        unionInsert(authorVOList, affiliationVOList, meshHeadingVOList, chemicalVOList, grantVOList, referenceVOList);
                    }
                }
            }
            // 각 리스트에 남아있는 데이터들 flush 용으로 한번 더 호출
            unionInsert(authorVOList, affiliationVOList, meshHeadingVOList, chemicalVOList, grantVOList, referenceVOList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void unionInsert(List<AuthorVO> authorVOList, List<AffiliationVO> affiliationVOList, List<MeshHeadingVO> meshHeadingVOList,
                             List<ChemicalVO> chemicalVOList, List<GrantVO> grantVOList, List<ReferenceVO> referenceVOList){
        if(!authorVOList.isEmpty()) {
            int cnt = pubmedService.unionInsertAuthor(authorVOList);
            authorVOList.clear();
        }
        if(!affiliationVOList.isEmpty()){
            int cnt = pubmedService.unionInsertAffiliation(affiliationVOList);
            affiliationVOList.clear();
        }
        if(!meshHeadingVOList.isEmpty()) {
            int cnt = pubmedService.unionInsertMeshHeading(meshHeadingVOList);
            meshHeadingVOList.clear();
        }
        if(!chemicalVOList.isEmpty()){
            int cnt = pubmedService.unionInsertChemical(chemicalVOList);
            chemicalVOList.clear();
        }
        if(!grantVOList.isEmpty()) {
            int cnt = pubmedService.unionInsertGrant(grantVOList);
            grantVOList.clear();
        }
        if(!referenceVOList.isEmpty()) {
            int cnt = pubmedService.unionInsertReference(referenceVOList);
            referenceVOList.clear();
        }
    }

    private void parsingArticleInfo(Element element){
        Element E_article = (Element) element.getElementsByTagName("Article").item(0);
        Element E_dateCompleted = (Element) element.getElementsByTagName("DateCompleted").item(0);
        Element E_dateRevised = (Element) element.getElementsByTagName("DateRevised").item(0);
        Element E_journalInfo = (Element) element.getElementsByTagName("MedlineJournalInfo").item(0);
        Element E_pubDate = (Element) E_article.getElementsByTagName("PubDate").item(0);
        Element E_pubmedData = (Element) element.getElementsByTagName("PubmedData").item(0);
        Element E_PMID = (Element) element.getElementsByTagName("PMID").item(0);
        Element E_articleIdList = (Element) E_pubmedData.getElementsByTagName("ArticleIdList").item(0);

        // DATE_COMPLETED, DATE_REVISED
        Date dateCompleted = getDateCompletedOrRevised(E_dateCompleted);
        Date dateRevised = getDateCompletedOrRevised(E_dateRevised);

        // ISSN_PRINT, ISSN_ELECTRONIC
        String[] issnArr = getIssn(E_article);
        String issnPrint = issnArr[0];
        String issnElectronic = issnArr[1];

        // MEDLINE_JOURNAL_COUNTRY, MEDLINE_JOURNAL_TA, NLM_UNIQUE_ID
        String country = E_journalInfo.getElementsByTagName("Country").item(0) != null
                ? E_journalInfo.getElementsByTagName("Country").item(0).getTextContent() : null;
        String ta = E_journalInfo.getElementsByTagName("MedlineTA").item(0) != null
                ? E_journalInfo.getElementsByTagName("MedlineTA").item(0).getTextContent() : null;
        String nlmUniqueId = E_journalInfo.getElementsByTagName("NlmUniqueID").item(0) != null
                ? E_journalInfo.getElementsByTagName("NlmUniqueID").item(0).getTextContent() : null;

        // VOLUME, ISSUE
        String volume = E_article.getElementsByTagName("Volume").item(0) != null
                ? E_article.getElementsByTagName("Volume").item(0).getTextContent() : null;
        String issue = E_article.getElementsByTagName("Issue").item(0) != null
                ? E_article.getElementsByTagName("Issue").item(0).getTextContent() : null;

        // PUB_YEAR, PUB_MONTH, PUB_DAY, MEDLINE_DATE, NORM_YM
        String[] pubDate = getPubDate(E_pubDate);
        String pubYear = pubDate[0];
        String pubMonth = pubDate[1];
        String pubDay = pubDate[2];
        String medlineDate = pubDate[3];
        String normYm = pubDate[4];

        // JOURNAL_TITLE
        String journalTitle = E_article.getElementsByTagName("Title").item(0) != null
                ? E_article.getElementsByTagName("Title").item(0).getTextContent() : null;
        // JOURNAL_TITLE_ABBR
        String isoAbbr = E_article.getElementsByTagName("ISOAbbreviation").item(0) != null
                ? E_article.getElementsByTagName("ISOAbbreviation").item(0).getTextContent() : null;
        // ARTICLE_TITLE
        String articleTitle = E_article.getElementsByTagName("ArticleTitle").item(0) != null
                ? E_article.getElementsByTagName("ArticleTitle").item(0).getTextContent() : null;
        // MEDLINE_PGN
        String medlinePgn = E_article.getElementsByTagName("MedlinePgn").item(0) != null
                ? E_article.getElementsByTagName("MedlinePgn").item(0).getTextContent() : null;
        // ABSTRACT
        String abstr = E_article.getElementsByTagName("Abstract").item(0) != null
                ? E_article.getElementsByTagName("Abstract").item(0).getTextContent() : null;
        // LANGUAGE
        String language = E_article.getElementsByTagName("Language").item(0) != null
                ? E_article.getElementsByTagName("Language").item(0).getTextContent() : null;

        // VERNACULAR_TITLE
        String vncTitle = E_article.getElementsByTagName("VernacularTitle").item(0) != null
                ? E_article.getElementsByTagName("VernacularTitle").item(0).getTextContent() : null;

        // PUBLICATION_STATUS
        String pubStatus = E_pubmedData.getElementsByTagName("PublicationStatus").item(0) != null
                ? E_pubmedData.getElementsByTagName("PublicationStatus").item(0).getTextContent() : null;

        // PMID, PMID_VERSION, PII, DOI
        int version = Integer.parseInt(E_PMID.getAttribute("Version"));
        String[] IDs = getIDs(E_articleIdList);
        String pmid = IDs[0];
        String pmcId = IDs[1];
        String pii = IDs[2];
        String doi = IDs[3];
        String mid = IDs[4];

        // REG_DATE
        Date regDate = new Date();
        // MOD_DATE
        Date modDate = new Date();

        PubmedArticleVO pubmedArticle =
                new PubmedArticleVO(dateCompleted, dateRevised, issnPrint, issnElectronic, volume, issue,
                        normYm, pubYear, pubMonth, pubDay, medlineDate, journalTitle, isoAbbr,
                        articleTitle, medlinePgn, abstr, language, vncTitle, country, ta, nlmUniqueId,
                        pubStatus, pmid, version, pmcId, pii, doi, mid, regDate, modDate);
        log.info("pubmedArticle: {}", pubmedArticle);

        // db insert
        PubmedArticleVO result = this.pubmedService.insertPubmedArticle(pubmedArticle);
    }

    private Date getDateCompletedOrRevised(Element element) {
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date result = null;

            if(element != null){
                String year = element.getElementsByTagName("Year").item(0).getTextContent();
                String month = element.getElementsByTagName("Month").item(0).getTextContent();
                String day = element.getElementsByTagName("Day").item(0).getTextContent();
                result = sdf.parse(year + "-" + month + "-" + day);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] getIssn(Element element) {
        NodeList issn = element.getElementsByTagName("ISSN");
        String[] issnArr = new String[2];
        for (int j = 0; j < issn.getLength(); j++) {
            NamedNodeMap attributes = issn.item(j).getAttributes();
            String issnType = attributes.getNamedItem("IssnType").getNodeValue();
            if (issnType.equals("Print"))       issnArr[0] = issn.item(j).getTextContent();
            if (issnType.equals("Electronic"))  issnArr[1] = issn.item(j).getTextContent();
        }
        return issnArr;
    }

    private String[] getPubDate(Element element){
        String pubYear = element.getElementsByTagName("Year").item(0) != null
                ? element.getElementsByTagName("Year").item(0).getTextContent() : null;
        String pubMonth = element.getElementsByTagName("Month").item(0) != null
                ? element.getElementsByTagName("Month").item(0).getTextContent() : null;
        String pubDay = element.getElementsByTagName("Day").item(0) != null
                ? element.getElementsByTagName("Day").item(0).getTextContent() : null;

        String medlineDate = element.getElementsByTagName("MedlineDate").item(0) != null
                ? element.getElementsByTagName("MedlineDate").item(0).getTextContent() : null;

        String normYm = "";
        if(pubYear != null){
            switch (pubMonth != null ? pubMonth : ""){
                case "Jan": normYm = pubYear + "01"; break;
                case "Feb": normYm = pubYear + "02"; break;
                case "Mar": normYm = pubYear + "03"; break;
                case "Apr": normYm = pubYear + "04"; break;
                case "May": normYm = pubYear + "05"; break;
                case "Jun": normYm = pubYear + "06"; break;
                case "Jul": normYm = pubYear + "07"; break;
                case "Aug": normYm = pubYear + "08"; break;
                case "Sep": normYm = pubYear + "09"; break;
                case "Oct": normYm = pubYear + "10"; break;
                case "Nov": normYm = pubYear + "11"; break;
                case "Dec": normYm = pubYear + "12"; break;
                default: normYm = pubYear;
            }

        } else if(medlineDate != null){
            String month = medlineDate.substring(4).trim();
            month = month.substring(0, 3);
            switch (month) {
                case "Jan": normYm=medlineDate.substring(0, 4) + "01"; break;
                case "Feb": normYm=medlineDate.substring(0, 4) + "02"; break;
                case "Mar": normYm=medlineDate.substring(0, 4) + "03"; break;
                case "Apr": normYm=medlineDate.substring(0, 4) + "04"; break;
                case "May": normYm=medlineDate.substring(0, 4) + "05"; break;
                case "Jun": normYm=medlineDate.substring(0, 4) + "06"; break;
                case "Jul": normYm=medlineDate.substring(0, 4) + "07"; break;
                case "Aug": normYm=medlineDate.substring(0, 4) + "08"; break;
                case "Sep": normYm=medlineDate.substring(0, 4) + "09"; break;
                case "Oct": normYm=medlineDate.substring(0, 4) + "10"; break;
                case "Nov": normYm=medlineDate.substring(0, 4) + "11"; break;
                case "Dec": normYm=medlineDate.substring(0, 4) + "12"; break;
                default: normYm=medlineDate.substring(0, 4);
            };

            for(char c : normYm.toCharArray()){
                if(!Character.isDigit(c)) {
                    normYm = medlineDate.substring(medlineDate.length()-4);
                    break;
                }
            }
        }
        return new String[]{pubYear, pubMonth, pubDay, medlineDate, normYm};
    }

    private static String[] getIDs(Element element) {
        NodeList articleIds = element.getElementsByTagName("ArticleId");
        String[] IDs = new String[5];
        for(int j=0; j<articleIds.getLength(); j++){
            NamedNodeMap attributes = articleIds.item(j).getAttributes();
            String idType = attributes.getNamedItem("IdType").getNodeValue();
            if(idType.equals("pubmed")) IDs[0] = articleIds.item(j).getTextContent();
            if(idType.equals("pmc"))    IDs[1] = articleIds.item(j).getTextContent();
            if(idType.equals("pii"))    IDs[2] = articleIds.item(j).getTextContent();
            if(idType.equals("doi"))    IDs[3] = articleIds.item(j).getTextContent();
            if(idType.equals("mid"))    IDs[4] = articleIds.item(j).getTextContent();
        }
        return IDs;
    }

    private void parsingAuthorAndMesh(Element element,
                                      List<AuthorVO> authorVOList,
                                      List<AffiliationVO> affiliationVOList,
                                      List<MeshHeadingVO> meshHeadingVOList){

        Element E_article = (Element) element.getElementsByTagName("Article").item(0);
        Element E_pubmedData = (Element) element.getElementsByTagName("PubmedData").item(0);
        Element E_articleIdList = (Element) E_pubmedData.getElementsByTagName("ArticleIdList").item(0);
        Element E_authorList = (Element) E_article.getElementsByTagName("AuthorList").item(0);
        Element E_meshHeadingList = (Element) element.getElementsByTagName("MeshHeadingList").item(0);

        String[] IDs = getIDs(E_articleIdList);
        String pmid = IDs[0];
        int articleId = this.pubmedService.getArticleId(pmid);

        // authorList
        if(E_authorList != null) {
            getAuthorList(E_authorList, articleId, pmid, authorVOList, affiliationVOList);
        }

        // meshKeyword
        if(E_meshHeadingList != null) {
            getMeshKeyword(E_meshHeadingList, articleId, pmid, meshHeadingVOList);
        }
    }

    private void getAuthorList(Element E_authorList, int articleId, String pmid,
                               List<AuthorVO> authorVOList,
                               List<AffiliationVO> affiliationVOList){

        NodeList authorList = E_authorList.getElementsByTagName("Author");
        for(int seq=0; seq<authorList.getLength(); seq++){
            Element E_author = (Element) authorList.item(seq);
            String validYn = E_author.getAttribute("ValidYN").equalsIgnoreCase("Y") ? "Y" : "N";
            String equalCont = E_author.getAttribute("EqualContrib").equalsIgnoreCase("Y") ? "Y" : null;
            String lastName = E_author.getElementsByTagName("LastName").item(0) != null ? E_author.getElementsByTagName("LastName").item(0).getTextContent() : null;
            String foreName = E_author.getElementsByTagName("ForeName").item(0) != null ? E_author.getElementsByTagName("ForeName").item(0).getTextContent() : null;
            String initials = E_author.getElementsByTagName("Initials").item(0) != null ? E_author.getElementsByTagName("Initials").item(0).getTextContent() : null;

            String affiliation;
            String email;
            NodeList affiliationInfoList = E_author.getElementsByTagName("AffiliationInfo");
            int affiliationLength = affiliationInfoList.getLength();
            if(affiliationLength > 0){
                for(int j=0; j<affiliationLength; j++){
                    Element E_affiliation = (Element) affiliationInfoList.item(j);

                    affiliation = E_affiliation.getTextContent();

                    if(affiliation.getBytes().length > 2000) {
                        try {
                            File file = new File("C:/logs/affiliationInfo.log");
                            if (!file.exists()) {
                                boolean create = file.createNewFile();
                            }

                            FileWriter fw = new FileWriter(file, true);
                            BufferedWriter bw = new BufferedWriter(fw);
                            bw.write("pmid: " + pmid + "\r\n");
                            bw.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    }

                    if(affiliation.lastIndexOf(" ") != -1){
                        email = affiliation.substring(affiliation.lastIndexOf(" ")+1);

                        String pattern = "^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}.$";
                        if(!Pattern.matches(pattern, email)){
                            email = null;
                        }else{
                            affiliation = affiliation.substring(0, affiliation.lastIndexOf(" ")+1);
                        }
                    }else{
                        email = null;
                    }

                    AffiliationVO affiliationVO = new AffiliationVO(articleId, seq+1, j+1, pmid, affiliation, email);
                    affiliationVOList.add(affiliationVO);
                }
            }

            AuthorVO authorVO = new AuthorVO(articleId, seq+1, pmid, validYn, equalCont, lastName, foreName, initials);
            authorVOList.add(authorVO);

        }
    }

    private void getMeshKeyword(Element E_meshHeadingList, int articleId, String pmid,
                                List<MeshHeadingVO> meshHeadingVOList){

        NodeList meshHeadingList = E_meshHeadingList.getElementsByTagName("MeshHeading");
        for(int i=0, seq=1; i<meshHeadingList.getLength(); i++){
            Element E_meshHeading = (Element) meshHeadingList.item(i);

            NodeList qualifierList = E_meshHeading.getElementsByTagName("QualifierName");
            if(qualifierList.getLength() > 0){
                for(int j=0; j<qualifierList.getLength(); j++){
                    Element descriptorName = (Element) E_meshHeading.getElementsByTagName("DescriptorName").item(0);
                    Element E_qualifier = (Element) qualifierList.item(j);

                    String meshTerms = descriptorName.getTextContent() + " / " + E_qualifier.getTextContent();
                    String meshUi = descriptorName.getAttribute("UI") + "_" + E_qualifier.getAttribute("UI");
                    String majorTopicYn = E_qualifier.getAttribute("MajorTopicYN").equals("Y") ? "Y" : "N";

                    MeshHeadingVO meshHeadingVO = new MeshHeadingVO(articleId, seq++, pmid, meshTerms, meshUi, majorTopicYn);
                    meshHeadingVOList.add(meshHeadingVO);
                }
            }else{
                Element descriptorName = (Element) E_meshHeading.getElementsByTagName("DescriptorName").item(0);
                String meshTerms = descriptorName.getTextContent();
                String meshUi = descriptorName.getAttribute("UI");
                String majorTopicYn = descriptorName.getAttribute("MajorTopicYN");

                MeshHeadingVO meshHeadingVO = new MeshHeadingVO(articleId, seq++, pmid, meshTerms, meshUi, majorTopicYn);
                meshHeadingVOList.add(meshHeadingVO);
            }
        }
    }

    private void parsingChemicalGrantReference(Element element,
                                               List<ChemicalVO> chemicalVOList,
                                               List<GrantVO> grantVOList,
                                               List<ReferenceVO> referenceVOList){
        Element E_article = (Element) element.getElementsByTagName("Article").item(0);
        Element E_pubmedData = (Element) element.getElementsByTagName("PubmedData").item(0);
        Element E_articleIdList = (Element) E_pubmedData.getElementsByTagName("ArticleIdList").item(0);
        Element E_chemicalList = (Element) element.getElementsByTagName("ChemicalList").item(0);
        Element E_grantList = (Element) E_article.getElementsByTagName("GrantList").item(0);
        Element E_referenceList = (Element) E_pubmedData.getElementsByTagName("ReferenceList").item(0);

        String[] IDs = getIDs(E_articleIdList);
        String pmid = IDs[0];
        int articleId = this.pubmedService.getArticleId(pmid);

        // chemicalList
        if(E_chemicalList != null) {
            getChemicalList(E_chemicalList, articleId, pmid, chemicalVOList);
        }

        // grantList
        if(E_grantList != null) {
            getGrantList(E_grantList, articleId, pmid, grantVOList);
        }

        // referenceList
        if(E_referenceList != null) {
            getReferenceList(E_referenceList, articleId, pmid, referenceVOList);
        }
    }

    private void getReferenceList(Element element, int articleId, String pmid, List<ReferenceVO> referenceVOList) {
        NodeList referenceList = element.getElementsByTagName("Reference");
        for(int i=0, seq=1; i<referenceList.getLength(); i++){
            String referencePmid = null;
            String referencePmcId = null;
            String referenceDoi = null;
            Element E_grant = (Element) referenceList.item(i);
            Element E_citation = (Element) E_grant.getElementsByTagName("Citation").item(0);
            Element E_articleIdList = (Element) E_grant.getElementsByTagName("ArticleIdList").item(0);
            NodeList articleIdTypeList = E_articleIdList != null ? (E_articleIdList).getElementsByTagName("ArticleId") : null;
            if(articleIdTypeList != null){
                for(int j=0; j<articleIdTypeList.getLength(); j++){
                    Element E_articleId = (Element) articleIdTypeList.item(j);
                    String idType = E_articleId != null ? E_articleId.getAttribute("IdType") : null;
                    String idValue = E_articleId != null ? E_articleId.getTextContent() : null;
                    if(idType != null){
                        switch (idType) {
                            case "pubmed": referencePmid = idValue; break;
                            case "pmcid": referencePmcId = idValue; break;
                            case "doi": referenceDoi = idValue; break;
                            default: {
                                String fileName = "anotherIdType.log";
                                String str = "articleId: " + articleId + "\treferenceSeq: " + seq + "\tpmid: " + pmid + "\tidType: " + idType + "\tvalue: " + idValue;
                                writeTxt(fileName, str);
                            }
                        }
                    }
                }
            }
            String citation = E_citation != null ? E_citation.getTextContent() : null;

            if(referencePmid != null && referencePmid.equals("NOT_FOUND;INVALID_JOURNAL"))   referencePmid = null;
            if(referencePmid != null && referencePmid.getBytes().length > 20) {
                String prefix = referencePmid.split("/")[0];
                try{
                    String suffix = referencePmid.split("/")[1];
                } catch (ArrayIndexOutOfBoundsException e){
                    referencePmid = null;
                }
                if(prefix.startsWith("10.") && (prefix.length()==7 || prefix.length()==8)) {
                    referenceDoi = referencePmid;
                    referencePmid = null;
                }
            }

            ReferenceVO referenceVO = new ReferenceVO(articleId, seq++, pmid, referencePmid, referencePmcId, referenceDoi, citation);
            referenceVOList.add(referenceVO);
        }
    }

    private void getGrantList(Element element, int articleId, String pmid, List<GrantVO> grantVOList) {
        NodeList grantList = element.getElementsByTagName("Grant");
        for(int i=0, seq=1; i<grantList.getLength(); i++){
            Element E_grant = (Element) grantList.item(i);

            String grantId = E_grant.getElementsByTagName("GrantID").item(0) != null ? E_grant.getElementsByTagName("GrantID").item(0).getTextContent() : null;
            String acronym = E_grant.getElementsByTagName("Acronym").item(0) != null ? E_grant.getElementsByTagName("Acronym").item(0).getTextContent() : null;
            String agency = E_grant.getElementsByTagName("Agency").item(0) != null ? E_grant.getElementsByTagName("Agency").item(0).getTextContent() : null;
            String country = E_grant.getElementsByTagName("Country").item(0) != null ? E_grant.getElementsByTagName("Country").item(0).getTextContent() : null;

            GrantVO grantVO = new GrantVO(articleId, seq++, pmid, grantId, acronym, agency, country);
            grantVOList.add(grantVO);
        }
        grantList.getLength();
    }

    private void getChemicalList(Element element, int articleId, String pmid, List<ChemicalVO> chemicalVOList) {
        NodeList chemicalList = element.getElementsByTagName("Chemical");
        for(int i=0, seq=1; i<chemicalList.getLength(); i++){
            Element E_chemical = (Element) chemicalList.item(i);
            Element E_substance = (Element) E_chemical.getElementsByTagName("NameOfSubstance").item(0);
            Element E_registryNumber = (Element) E_chemical.getElementsByTagName("RegistryNumber").item(0);

            String registryNumber = E_registryNumber != null ? E_registryNumber.getTextContent() : null;
            String chemicalSubstance = E_substance != null ? E_substance.getTextContent() : null;
            String chemicalUi = E_substance != null ? E_substance.getAttribute("UI") : null;

            ChemicalVO chemicalVO = new ChemicalVO(articleId, seq++, pmid, registryNumber, chemicalSubstance, chemicalUi);
            chemicalVOList.add(chemicalVO);
        }
    }

    private void writeTxt(String file, String str){
        if(!file.startsWith("C:/") || !file.startsWith("c:/")) file = "C:/logs/"+file;
        try {
            File txtFile = new File(file);
            if (!txtFile.exists()) {
                boolean create = txtFile.createNewFile();
                log.info("{}", create);
            }

            FileWriter fw = new FileWriter(txtFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(str + "\r\n");
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
