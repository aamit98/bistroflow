package DomainLayer.HR;

import DataAccessLayer.HR.ControllerClasses.EmployeeAvailabilityController;
import DataAccessLayer.HR.DTOClasses.EmployeeAvailabilityDTO;

/**
 * this class is used to describe an employee availability (in a particular week)
 * in array true means can work said shift
 * for example, if availabilityArray[1][0]==true, the employee can work monday morning shift.
 */
public class EmployeeAvailability {
    private final boolean[][] availabilityArray;
    private final EmployeeAvailabilityDTO employeeAvailabilityDTO;

    /**
     *
     * @param availabilityArray - should be size [7][2]. first index-day. second index-shift(morning/evening)
     * @throws Exception if given wrong size / null in any array.
     */
    public EmployeeAvailability(boolean[][] availabilityArray, int employeeId, int date,
                                EmployeeAvailabilityController controller, boolean fromDB) throws Exception {
        if(availabilityArray == null || availabilityArray.length==7)
        {
            for(int i=0;i<7;i++)
            {
                if(availabilityArray[i]==null || availabilityArray[i].length!=2)
                {
                    throw new Exception("wrong array size!");
                }
            }
        }
        else
        {
            throw new Exception("wrong array size!");
        }
        this.availabilityArray = availabilityArray.clone();
        employeeAvailabilityDTO = new EmployeeAvailabilityDTO(employeeId,date,this.availabilityArray,controller,fromDB);

    }
    public boolean[][] getAvailabilityArray()
    {
        return availabilityArray.clone();
    }
    public void persistDTO() throws Exception {
        employeeAvailabilityDTO.persist();
    }
    public void deleteDTO() throws Exception {
        employeeAvailabilityDTO.deleteAvailability();
    }

}
