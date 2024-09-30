package com.argonet.pubmed.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorVO {
    private int articleId;
    private int authorSeq;
    private String pmid;
    private String validYn;
    private String equalContrib;
    private String lastName;
    private String foreName;
    private String initials;
}
