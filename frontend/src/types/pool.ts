import { PoolRanking } from '@/src/types/poolRanking';


export interface Pool {
  id?: number;
  name: string;
  poolRanking: PoolRanking;
}
