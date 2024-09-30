package com.argonet.pubmed.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AffiliationVO {
    private int articleId;
    private int authorSeq;
    private int affiliationSeq;
    private String pmid;
    private String affiliationInfo;
    private String authorEmail;
}
