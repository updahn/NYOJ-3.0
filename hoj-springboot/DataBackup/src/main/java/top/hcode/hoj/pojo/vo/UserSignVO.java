package top.hcode.hoj.pojo.vo;

import java.util.Date;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserSignVO {

    private Long id;

    @ApiModelProperty(value = "用户id")
    private String uid;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "真实姓名")
    private String realname;

    @ApiModelProperty(value = "英文姓名")
    private String englishname;

    @ApiModelProperty(value = "学校")
    private String school;

    @ApiModelProperty(value = "院系")
    private String faculty;

    @ApiModelProperty(value = "专业/班级")
    private String course;

    @ApiModelProperty(value = "学号")
    private String number;

    @ApiModelProperty(value = "衣服尺寸")
    private String clothesSize;

    @ApiModelProperty(value = "联系方式")
    private String phoneNumber;

    @ApiModelProperty(value = "入学年份")
    private Date stSchool;

    @ApiModelProperty(value = "毕业年份")
    private Date edSchool;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "性别")
    private String gender;

    @ApiModelProperty(value = "1 表示已经被成功邀请，2表示拒绝邀请且还未加入队伍，-1表示没有邀请")
    private Integer status;

    @ApiModelProperty(value = "邀请信息")
    private String content;

    @ApiModelProperty(value = "教练/队长信息")
    private List<CoachInfoVO> coachInfoVoList;

    @ApiModelProperty(value = "创建时间")
    private Date gmtCreate;

}