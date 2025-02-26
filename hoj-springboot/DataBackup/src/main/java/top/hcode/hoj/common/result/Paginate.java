package top.hcode.hoj.common.result;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.Data;

@Data
public class Paginate<T> {

    /**
     * 通用分页方法，返回 IPage 对象
     *
     * @param <T>         数据类型
     * @param list        完整的数据列表
     * @param currentPage 当前页码，从 1 开始
     * @param pageSize    每页显示的记录数
     * @return IPage 分页结果
     */
    public static <T> IPage<T> paginateListToIPage(List<T> list, Integer currentPage, Integer pageSize) {
        int total = list.size();

        if (pageSize == null) {
            Page<T> page = new Page<>(1, total, total);
            page.setRecords(list);
            return page;
        }

        int startIndex = Math.max(0, (currentPage - 1) * pageSize);
        int endIndex = Math.min(startIndex + pageSize, total);
        List<T> paginatedList = new ArrayList<>(list.subList(startIndex, endIndex));

        Page<T> page = new Page<>(currentPage, pageSize, total);
        page.setRecords(paginatedList);
        return page;
    }

    /**
     * 分页处理排名列表数据，返回 Page 对象
     *
     * @param <T>         数据类型
     * @param list        排名列表
     * @param currentPage 当前页码，从 1 开始
     * @param pageSize    每页显示的记录数
     * @return Page 分页结果
     */
    public static <T> Page<T> paginateRankListToPage(List<T> list, Integer currentPage, Integer pageSize) {
        int total = list.size();
        int startIndex = Math.max(0, (currentPage - 1) * pageSize);
        int endIndex = Math.min(startIndex + pageSize, total);
        List<T> paginatedList = new ArrayList<>(list.subList(startIndex, endIndex));

        Page<T> page = new Page<>(currentPage, pageSize, total);
        page.setRecords(paginatedList);
        return page;
    }

    /**
     * 使用现有的 IPage 对象和一个新的 List<T> 列表进行分页，并返回更新后的 IPage 对象
     *
     * @param <T>  数据类型
     * @param page 现有的 IPage 对象
     * @param list 新的数据列表
     * @return IPage 分页结果
     */
    public static <T> IPage<T> paginateWithExistingPage(IPage<T> page, List<T> list) {
        int total = list.size();
        int currentPage = (int) page.getCurrent();
        int pageSize = (int) page.getSize();

        int startIndex = Math.max(0, (currentPage - 1) * pageSize);
        int endIndex = Math.min(startIndex + pageSize, total);
        List<T> paginatedList = new ArrayList<>(list.subList(startIndex, endIndex));

        page.setRecords(paginatedList);
        page.setTotal(total);

        return page;
    }
}
