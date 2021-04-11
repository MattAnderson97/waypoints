package space.wolv.waypoints;

import community.leaf.textchain.adventure.TextChain;

import java.util.ArrayList;
import java.util.List;

public class Utils
{
    public static List<List<TextChain>> paginate(List<TextChain> list, int pageSize)
    {
        List<List<TextChain>> pages = new ArrayList<>();
        int maxPages;

        if (list.size() >= pageSize)
        {
            // maxPages = list.size() / pageSize + ((list.size() / pageSize == 0) ? 0 : 1);
            maxPages = (int) Math.ceil((double) list.size() / pageSize);
        }
        else
        {
            maxPages = 1;
        }

        for (int i = 0; i < maxPages; i++)
        {
            List<TextChain> page = new ArrayList<>();
            int maxItems = Math.min(pageSize, list.size());
            for (int j = 0; j < maxItems; j++)
            {
                TextChain curValue = list.get(0);
                list.remove(0);
                page.add(curValue);
            }

            pages.add(page);
        }

        return pages;
    }
}
