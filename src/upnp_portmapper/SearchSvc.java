/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upnp_portmapper;

/**
 *
 * @author éª?
 */
public class SearchSvc{
    /**
     * This method will search whole szSource from lIndex to szSource.length()
     * to match szModel case sensity,if exist,return the array index,if fail,
     * return -1
     * @param szSource The string want to search
     * @param szModel The string model
     * @param lIndex Start position
     * @return Model's index in array
     */
    public static int KMP(String szSource,String szModel,int lIndex){
        if ((szSource.length() == 0)||(szModel.length() == 0))
            return -1;
        if (lIndex > (szSource.length()-1))
            return -1;
        int ptrS = lIndex;
        int ptrM = 0;
        int Next[] = KMP_getNext(szModel);
        while(ptrS<szSource.length()){
            if( (ptrM<0) || (szSource.charAt(ptrS) == szModel.charAt(ptrM)) ){
                ++ptrS;
                ++ptrM;
            }
            else
                ptrM = Next[ptrM];
            if (ptrM == szModel.length())
                return ptrS-szModel.length();
        }
        return -1;
    }
    
    public static int[] KMP_getNext(String szModel){
        int Next[] = new int[szModel.length()];
        Next[0] = -1;
        int ptrVal = -1;
        int ptrCrt = 0;
        while(ptrCrt<szModel.length()-1){
            if ((ptrVal==-1)||szModel.charAt(ptrCrt)==szModel.charAt(ptrVal)){
                ++ptrVal;
                ++ptrCrt;
                Next[ptrCrt] = ptrVal;
            }
            else
                ptrVal = Next[ptrVal];
        }
        
        return Next;
    }
    
    /**
     * This method will search whole szSource from lIndex to szSource.length()
     * to match szModel case insensity,if exist,return the array index,if fail,
     * return -1
     * @param szSource The string want to search
     * @param szModel The string model
     * @param lIndex Start position
     * @return Model's index in array
     */
    public static int KMP_IgnoreCaption(String szSource,String szModel,int lIndex){
        if ((szSource.length() == 0)||(szModel.length() == 0))
            return -1;
        if (lIndex > (szSource.length()-1))
            return -1;
        int ptrS = lIndex;
        int ptrM = 0;
        int Next[] = KMP_IgnoreCaption_getNext(szModel);
        while(ptrS<szSource.length()){
            if( (ptrM<0) || (szSource.charAt(ptrS) == szModel.charAt(ptrM)) || (szSource.charAt(ptrS) == szModel.charAt(ptrM)+32) || (szSource.charAt(ptrS)+32 == szModel.charAt(ptrM))){
                ++ptrS;
                ++ptrM;
            }
            else
                ptrM = Next[ptrM];
            if (ptrM == szModel.length())
                return ptrS-szModel.length();
        }
        return -1;
    }
    
    public static int[] KMP_IgnoreCaption_getNext(String szModel){
        int Next[] = new int[szModel.length()];
        Next[0] = -1;
        int ptrVal = -1;
        int ptrCrt = 0;
        while(ptrCrt<szModel.length()-1){
            if ((ptrVal==-1) || (szModel.charAt(ptrCrt)==szModel.charAt(ptrVal)) || (szModel.charAt(ptrCrt)==szModel.charAt(ptrVal)+32) || (szModel.charAt(ptrCrt)+32==szModel.charAt(ptrVal))){
                ++ptrVal;
                ++ptrCrt;
                Next[ptrCrt] = ptrVal;
            }
            else
                ptrVal = Next[ptrVal];
        }
        
        return Next;
    }
}