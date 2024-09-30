package com.argonet.pubmed.service;

import com.argonet.pubmed.mapper.PubmedMapper;
import com.argonet.pubmed.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PubmedService {
    @Autowired
    PubmedMapper pubmedMapper;

    public int getArticleId(String pmid) {
        return this.pubmedMapper.getArticleId(pmid);
    }

    public PubmedArticleVO insertPubmedArticle(PubmedArticleVO pubmedArticle) {
        int cnt = 0;
        int savedVersion = this.pubmedMapper.getSavedVersion(pubmedArticle.getPmid());
        if(savedVersion < pubmedArticle.getPmidVersion()){
            cnt += this.pubmedMapper.insertPubmedArticle(pubmedArticle);
        }else{
            cnt += this.pubmedMapper.deleteAffiliation(pubmedArticle.getPmid());
            cnt += this.pubmedMapper.deleteAuthor(pubmedArticle.getPmid());
            cnt += this.pubmedMapper.deleteMeshHeading(pubmedArticle.getPmid());
            cnt += this.pubmedMapper.deleteChemical(pubmedArticle.getPmid());
            cnt += this.pubmedMapper.deleteGrant(pubmedArticle.getPmid());
            cnt += this.pubmedMapper.deleteReference(pubmedArticle.getPmid());
            cnt += this.pubmedMapper.updatePubmedArticle(pubmedArticle);
        }
        if(cnt > 0) return pubmedArticle;
        else        return null;
    }

    public int unionInsertAuthor(List<AuthorVO> authorVOList) {
        return this.pubmedMapper.unionInsertAuthor(authorVOList);
    }

    public int unionInsertAffiliation(List<AffiliationVO> affiliationVOList) {
        return this.pubmedMapper.unionInsertAffiliation(affiliationVOList);
    }

    public int unionInsertMeshHeading(List<MeshHeadingVO> meshHeadingVOList) {
        return this.pubmedMapper.unionInsertMeshHeading(meshHeadingVOList);
    }

    public int unionInsertChemical(List<ChemicalVO> chemicalVOList) {
        return this.pubmedMapper.unionInsertChemical(chemicalVOList);
    }

    public int unionInsertGrant(List<GrantVO> grantVOList) {
        return this.pubmedMapper.unionInsertGrant(grantVOList);
    }

    public int unionInsertReference(List<ReferenceVO> referenceVOList) {
        return this.pubmedMapper.unionInsertReference(referenceVOList);
    }
}