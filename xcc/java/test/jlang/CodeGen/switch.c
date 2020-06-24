int bar(int val)
{
    int res = 0;
    switch(val)
    {
	case 1:
	    res += 1;
	    break;
	case 2:
	    res += 2;
	    break;
	default:
	    break;
    }
    return res;
}
