package com.argonet.pubmed.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReferenceVO {
    private int articleId;
    private int referenceSeq;
    private String pmid;
    private String referencePmid;
    private String referencePmcId;
    private String referenceDoi;
    private String citation;
}
