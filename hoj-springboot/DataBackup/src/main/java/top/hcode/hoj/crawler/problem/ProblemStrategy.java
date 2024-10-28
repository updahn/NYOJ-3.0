package top.hcode.hoj.crawler.problem;

import lombok.Data;
import lombok.experimental.Accessors;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.pojo.entity.problem.Tag;
import top.hcode.hoj.utils.Constants;

import java.util.List;

public abstract class ProblemStrategy {

    public abstract RemoteProblemInfo getProblemInfo(String problemId, String author) throws Exception;

    public RemoteProblemInfo getProblemInfoByLogin(String problemId, String author, String username, String password)
            throws Exception {
        return null;
    }

    @Data
    @Accessors(chain = true)
    public static class RemoteProblemInfo {
        private Problem problem;
        private List<ProblemDescription> problemDescriptionList;
        private List<Tag> tagList;
        private List<String> langIdList;
        private List<Pair_<String, String>> langList;
        private Constants.RemoteOJ remoteOJ;
    }
}
