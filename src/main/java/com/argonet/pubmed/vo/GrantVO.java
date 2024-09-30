package com.argonet.pubmed.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrantVO {
    private int articleId;
    private int grantSeq;
    private String pmid;
    private String grantId;
    private String acronym;
    private String agency;
    private String country;
}
