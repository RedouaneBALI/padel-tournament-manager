package io.github.redouanebali.mapper;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.request.CreateTournamentRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.PoolRankingDetailsDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.ScoreDTO;
import io.github.redouanebali.dto.response.SetScoreDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.PoolRanking;
import io.github.redouanebali.model.PoolRankingDetails;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-05T11:30:49+0100",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class TournamentMapperImpl implements TournamentMapper {

    @Override
    public TournamentDTO toDTO(Tournament tournament) {
        if ( tournament == null ) {
            return null;
        }

        TournamentDTO tournamentDTO = new TournamentDTO();

        tournamentDTO.setRounds( toDTORoundList( tournament.getRounds() ) );
        tournamentDTO.setPlayerPairs( toDTOPlayerPairList( tournament.getPlayerPairs() ) );
        tournamentDTO.setId( tournament.getId() );
        tournamentDTO.setOwnerId( tournament.getOwnerId() );
        tournamentDTO.setName( tournament.getName() );
        tournamentDTO.setDescription( tournament.getDescription() );
        tournamentDTO.setCity( tournament.getCity() );
        tournamentDTO.setClub( tournament.getClub() );
        tournamentDTO.setGender( tournament.getGender() );
        tournamentDTO.setLevel( tournament.getLevel() );
        tournamentDTO.setConfig( tournament.getConfig() );
        tournamentDTO.setStartDate( tournament.getStartDate() );
        tournamentDTO.setEndDate( tournament.getEndDate() );

        return tournamentDTO;
    }

    @Override
    public List<TournamentDTO> toDTO(List<Tournament> tournaments) {
        if ( tournaments == null ) {
            return null;
        }

        List<TournamentDTO> list = new ArrayList<TournamentDTO>( tournaments.size() );
        for ( Tournament tournament : tournaments ) {
            list.add( toDTO( tournament ) );
        }

        return list;
    }

    @Override
    public Set<TournamentDTO> toDTO(Set<Tournament> tournaments) {
        if ( tournaments == null ) {
            return null;
        }

        Set<TournamentDTO> set = LinkedHashSet.newLinkedHashSet( tournaments.size() );
        for ( Tournament tournament : tournaments ) {
            set.add( toDTO( tournament ) );
        }

        return set;
    }

    @Override
    public GameDTO toDTO(Game game) {
        if ( game == null ) {
            return null;
        }

        GameDTO gameDTO = new GameDTO();

        gameDTO.setWinnerSide( game.getWinnerSide() );
        gameDTO.setId( game.getId() );
        gameDTO.setTeamA( toDTO( game.getTeamA() ) );
        gameDTO.setTeamB( toDTO( game.getTeamB() ) );
        gameDTO.setFinished( game.isFinished() );
        gameDTO.setScore( scoreToScoreDTO( game.getScore() ) );

        return gameDTO;
    }

    @Override
    public List<GameDTO> toDTOGameList(List<Game> games) {
        if ( games == null ) {
            return null;
        }

        List<GameDTO> list = new ArrayList<GameDTO>( games.size() );
        for ( Game game : games ) {
            list.add( toDTO( game ) );
        }

        return list;
    }

    @Override
    public Set<GameDTO> toDTOGameSet(Set<Game> games) {
        if ( games == null ) {
            return null;
        }

        Set<GameDTO> set = LinkedHashSet.newLinkedHashSet( games.size() );
        for ( Game game : games ) {
            set.add( toDTO( game ) );
        }

        return set;
    }

    @Override
    public List<PlayerPairDTO> toDTOPlayerPairList(List<PlayerPair> playerPairs) {
        if ( playerPairs == null ) {
            return null;
        }

        List<PlayerPairDTO> list = new ArrayList<PlayerPairDTO>( playerPairs.size() );
        for ( PlayerPair playerPair : playerPairs ) {
            list.add( toDTO( playerPair ) );
        }

        return list;
    }

    @Override
    public Set<PlayerPairDTO> toDTOPlayerPairSet(Set<PlayerPair> playerPairs) {
        if ( playerPairs == null ) {
            return null;
        }

        Set<PlayerPairDTO> set = LinkedHashSet.newLinkedHashSet( playerPairs.size() );
        for ( PlayerPair playerPair : playerPairs ) {
            set.add( toDTO( playerPair ) );
        }

        return set;
    }

    @Override
    public RoundDTO toDTO(Round round) {
        if ( round == null ) {
            return null;
        }

        RoundDTO roundDTO = new RoundDTO();

        roundDTO.setId( round.getId() );
        roundDTO.setStage( round.getStage() );
        roundDTO.setGames( toDTOGameList( round.getGames() ) );
        roundDTO.setMatchFormat( toDTO( round.getMatchFormat() ) );
        roundDTO.setPools( toDTOPoolList( round.getPools() ) );

        return roundDTO;
    }

    @Override
    public List<RoundDTO> toDTORoundList(List<Round> rounds) {
        if ( rounds == null ) {
            return null;
        }

        List<RoundDTO> list = new ArrayList<RoundDTO>( rounds.size() );
        for ( Round round : rounds ) {
            list.add( toDTO( round ) );
        }

        return list;
    }

    @Override
    public MatchFormatDTO toDTO(MatchFormat matchFormat) {
        if ( matchFormat == null ) {
            return null;
        }

        MatchFormatDTO matchFormatDTO = new MatchFormatDTO();

        matchFormatDTO.setNumberOfSetsToWin( matchFormat.getNumberOfSetsToWin() );
        matchFormatDTO.setGamesPerSet( matchFormat.getGamesPerSet() );
        matchFormatDTO.setAdvantage( matchFormat.isAdvantage() );
        matchFormatDTO.setSuperTieBreakInFinalSet( matchFormat.isSuperTieBreakInFinalSet() );

        return matchFormatDTO;
    }

    @Override
    public List<MatchFormatDTO> toDTOMatchFormatList(List<MatchFormat> matchFormats) {
        if ( matchFormats == null ) {
            return null;
        }

        List<MatchFormatDTO> list = new ArrayList<MatchFormatDTO>( matchFormats.size() );
        for ( MatchFormat matchFormat : matchFormats ) {
            list.add( toDTO( matchFormat ) );
        }

        return list;
    }

    @Override
    public Set<MatchFormatDTO> toDTOMatchFormatSet(Set<MatchFormat> matchFormats) {
        if ( matchFormats == null ) {
            return null;
        }

        Set<MatchFormatDTO> set = LinkedHashSet.newLinkedHashSet( matchFormats.size() );
        for ( MatchFormat matchFormat : matchFormats ) {
            set.add( toDTO( matchFormat ) );
        }

        return set;
    }

    @Override
    public PoolDTO toDTO(Pool pool) {
        if ( pool == null ) {
            return null;
        }

        PoolDTO poolDTO = new PoolDTO();

        poolDTO.setPairs( toDTOPlayerPairList( pool.getPairs() ) );
        poolDTO.setPoolRanking( toDTO( pool.getPoolRanking() ) );
        poolDTO.setId( pool.getId() );
        poolDTO.setName( pool.getName() );

        return poolDTO;
    }

    @Override
    public List<PoolDTO> toDTOPoolList(List<Pool> pools) {
        if ( pools == null ) {
            return null;
        }

        List<PoolDTO> list = new ArrayList<PoolDTO>( pools.size() );
        for ( Pool pool : pools ) {
            list.add( toDTO( pool ) );
        }

        return list;
    }

    @Override
    public PoolRankingDTO toDTO(PoolRanking poolRanking) {
        if ( poolRanking == null ) {
            return null;
        }

        PoolRankingDTO poolRankingDTO = new PoolRankingDTO();

        poolRankingDTO.setId( poolRanking.getId() );
        poolRankingDTO.setDetails( toDTOPoolRankingDetailsList( poolRanking.getDetails() ) );

        return poolRankingDTO;
    }

    @Override
    public List<PoolRankingDTO> toDTOPoolRankingList(List<PoolRanking> poolRankings) {
        if ( poolRankings == null ) {
            return null;
        }

        List<PoolRankingDTO> list = new ArrayList<PoolRankingDTO>( poolRankings.size() );
        for ( PoolRanking poolRanking : poolRankings ) {
            list.add( toDTO( poolRanking ) );
        }

        return list;
    }

    @Override
    public PoolRankingDetailsDTO toDTO(PoolRankingDetails details) {
        if ( details == null ) {
            return null;
        }

        PoolRankingDetailsDTO poolRankingDetailsDTO = new PoolRankingDetailsDTO();

        poolRankingDetailsDTO.setPairId( detailsPlayerPairId( details ) );
        poolRankingDetailsDTO.setPlayerPair( toDTO( details.getPlayerPair() ) );
        poolRankingDetailsDTO.setPoints( details.getPoints() );
        poolRankingDetailsDTO.setSetAverage( details.getSetAverage() );

        return poolRankingDetailsDTO;
    }

    @Override
    public List<PoolRankingDetailsDTO> toDTOPoolRankingDetailsList(List<PoolRankingDetails> details) {
        if ( details == null ) {
            return null;
        }

        List<PoolRankingDetailsDTO> list = new ArrayList<PoolRankingDetailsDTO>( details.size() );
        for ( PoolRankingDetails poolRankingDetails : details ) {
            list.add( toDTO( poolRankingDetails ) );
        }

        return list;
    }

    @Override
    public List<PlayerPair> toPlayerPairList(List<CreatePlayerPairRequest> requests) {
        if ( requests == null ) {
            return null;
        }

        List<PlayerPair> list = new ArrayList<PlayerPair>( requests.size() );
        for ( CreatePlayerPairRequest createPlayerPairRequest : requests ) {
            list.add( toPlayerPair( createPlayerPairRequest ) );
        }

        return list;
    }

    @Override
    public Tournament toEntity(CreateTournamentRequest request) {
        if ( request == null ) {
            return null;
        }

        Tournament tournament = new Tournament();

        tournament.setName( request.getName() );
        tournament.setDescription( request.getDescription() );
        tournament.setCity( request.getCity() );
        tournament.setClub( request.getClub() );
        tournament.setGender( request.getGender() );
        tournament.setLevel( request.getLevel() );
        tournament.setStartDate( request.getStartDate() );
        tournament.setEndDate( request.getEndDate() );
        tournament.setConfig( request.getConfig() );

        return tournament;
    }

    protected SetScoreDTO setScoreToSetScoreDTO(SetScore setScore) {
        if ( setScore == null ) {
            return null;
        }

        SetScoreDTO setScoreDTO = new SetScoreDTO();

        setScoreDTO.setTeamAScore( setScore.getTeamAScore() );
        setScoreDTO.setTeamBScore( setScore.getTeamBScore() );
        setScoreDTO.setTieBreakTeamA( setScore.getTieBreakTeamA() );
        setScoreDTO.setTieBreakTeamB( setScore.getTieBreakTeamB() );

        return setScoreDTO;
    }

    protected List<SetScoreDTO> setScoreListToSetScoreDTOList(List<SetScore> list) {
        if ( list == null ) {
            return null;
        }

        List<SetScoreDTO> list1 = new ArrayList<SetScoreDTO>( list.size() );
        for ( SetScore setScore : list ) {
            list1.add( setScoreToSetScoreDTO( setScore ) );
        }

        return list1;
    }

    protected ScoreDTO scoreToScoreDTO(Score score) {
        if ( score == null ) {
            return null;
        }

        ScoreDTO scoreDTO = new ScoreDTO();

        scoreDTO.setSets( setScoreListToSetScoreDTOList( score.getSets() ) );

        return scoreDTO;
    }

    private Long detailsPlayerPairId(PoolRankingDetails poolRankingDetails) {
        PlayerPair playerPair = poolRankingDetails.getPlayerPair();
        if ( playerPair == null ) {
            return null;
        }
        return playerPair.getId();
    }
}
