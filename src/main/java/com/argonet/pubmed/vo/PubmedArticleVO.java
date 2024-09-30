package com.argonet.pubmed.vo;

import lombok.Data;

import java.util.Date;

@Data
public class PubmedArticleVO {
    private int articleId;
    private Date dateCompleted;
    private Date dateRevised;
    private String issnPrint;
    private String issnElectronic;
    private String volume;
    private String issue;
    private String normYear;
    private String pubYear;
    private String pubMonth;
    private String pubDay;
    private String medlineDate;
    private String journalTitle;
    private String journalTitleAbbr;
    private String articleTitle;
    private String medlinePgn;
    private String abstr;
    private String language;
    private String vernacularTitle;
    private String medlineJournalCountry;
    private String medlineJournalTa;
    private String medlineJournalNlmId;
    private String publicationStatus;
    private String pmid;
    private int pmidVersion;
    private String pmcId;
    private String pii;
    private String doi;
    private String mid;
    private Date regDate;
    private Date modDate;

    public PubmedArticleVO(Date dateCompleted, Date dateRevised, String issnPrint, String issnElectronic, String volume, String issue,
                           String normYear, String pubYear, String pubMonth, String pubDay, String medlineDate, String journalTitle, String journalTitleAbbr,
                           String articleTitle, String medlinePgn, String abstr, String language, String vernacularTitle,
                           String medlineJournalCountry, String medlineJournalTa, String medlineJournalNlmId, String publicationStatus,
                           String pmid, int pmidVersion, String pmcId, String pii, String doi, String mid, Date regDate, Date modDate) {
        this.dateCompleted = dateCompleted;
        this.dateRevised = dateRevised;
        this.issnPrint = issnPrint;
        this.issnElectronic = issnElectronic;
        this.volume = volume;
        this.issue = issue;
        this.normYear = normYear;
        this.pubYear = pubYear;
        this.pubMonth = pubMonth;
        this.pubDay = pubDay;
        this.medlineDate = medlineDate;
        this.journalTitle = journalTitle;
        this.journalTitleAbbr = journalTitleAbbr;
        this.articleTitle = articleTitle;
        this.medlinePgn = medlinePgn;
        this.abstr = abstr;
        this.language = language;
        this.vernacularTitle = vernacularTitle;
        this.medlineJournalCountry = medlineJournalCountry;
        this.medlineJournalTa = medlineJournalTa;
        this.medlineJournalNlmId = medlineJournalNlmId;
        this.publicationStatus = publicationStatus;
        this.pmid = pmid;
        this.pmidVersion = pmidVersion;
        this.pmcId = pmcId;
        this.pii = pii;
        this.doi = doi;
        this.mid = mid;
        this.regDate = regDate;
        this.modDate = modDate;
    }
}